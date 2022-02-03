package mb.statix.constraints.messages;

import com.google.common.collect.ImmutableMap;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.SolverResult;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MessageUtil {

    // @formatter:off
    private static final Map<Class<? extends IConstraint>, MessageKind> KINDS =
        ImmutableMap.<Class<? extends IConstraint>, MessageKind>builder()
            .put(CAstId.class, MessageKind.IGNORE)
            .put(CAstProperty.class, MessageKind.IGNORE)
            .build();
    // @formatter:on

    public static IMessage findClosestMessage(IConstraint c) {
        return findClosestMessage(c, KINDS.getOrDefault(c.getClass(), MessageKind.ERROR));
    }

    /**
     * Find closest message in the
     */
    public static IMessage findClosestMessage(IConstraint c, MessageKind kind) {
        @Nullable IMessage message = null;
        while(c != null) {
            @Nullable IMessage m;
            if((m = c.message().orElse(null)) != null && (message == null || message.kind().isWorseThan(m.kind()))) {
                message = m;
            }
            c = c.cause().orElse(null);
        }
        if(message == null) {
            message = new Message(kind);
        }
        return message;
    }

    public static SolverResult delaysAsErrors(SolverResult result, boolean suppressCascadingErrors) {

        ImmutableMap<IConstraint, Delay> delays = result.delays();

        if(suppressCascadingErrors) {
            // Collect all delays that involve variables that do not occur on free variables.

            // Collect all free variables in 'real' errors
            // @formatter:off
            final IUniDisunifier.Immutable unifier = result.state().unifier();
            Set.Immutable<ITermVar> newErrorVars = result.messages().entrySet().stream()
                .filter(e -> e.getValue().kind().equals(MessageKind.ERROR))
                .map(Map.Entry::getKey)
                .flatMap(c -> c.freeVars().stream())
                .flatMap(v -> unifier.findRecursive(v).getVars().stream())
                .collect(CapsuleCollectors.toSet());
            // @formatter:on
            Set.Immutable<ITermVar> allErrorVars = newErrorVars;

            Set.Immutable<CriticalEdge> newCriticalEdges = CapsuleUtil.immutableSet();
            Set.Immutable<CriticalEdge> allCriticalEdges = CapsuleUtil.immutableSet();

            while(!newErrorVars.isEmpty() || !newCriticalEdges.isEmpty()) {
                final Set.Transient<ITermVar> _newVars = CapsuleUtil.transientSet();
                final Set.Transient<CriticalEdge> _newCriticalEdges = CapsuleUtil.transientSet();
                final ImmutableMap.Builder<IConstraint, Delay> retainedDelays = ImmutableMap.builder();

                for(Map.Entry<IConstraint, Delay> e : delays.entrySet()) {
                    Delay d = e.getValue();
                    if(d.vars().stream().anyMatch(newErrorVars::contains)
                        || d.criticalEdges().stream().anyMatch(newCriticalEdges::contains)) {
                        for(ITermVar var : e.getKey().freeVars()) {
                            _newVars.__insertAll(unifier.findRecursive(var).getVars().__removeAll(allErrorVars));
                        }
                        for(Map.Entry<ITerm, MultiSet.Immutable<EdgeOrData<ITerm>>> criticalEdges : e.getKey()
                            .ownCriticalEdges().orElse(Completeness.Immutable.of()).entrySet()) {
                            final ITerm scope = unifier.findRecursive(criticalEdges.getKey());
                            final Set.Immutable<CriticalEdge> edges = criticalEdges.getValue().elementSet().stream()
                                .map(edge -> CriticalEdge.of(scope, edge)).collect(CapsuleCollectors.toSet());
                            _newCriticalEdges.__insertAll(edges.__removeAll(allCriticalEdges));
                        }
                    } else {
                        retainedDelays.put(e);
                    }
                }

                delays = retainedDelays.build();
                newErrorVars = _newVars.freeze();
                allErrorVars = allErrorVars.__insertAll(newErrorVars);

                newCriticalEdges = _newCriticalEdges.freeze();
                allCriticalEdges = allCriticalEdges.__insertAll(newCriticalEdges);
            }
        }

        final ImmutableMap.Builder<IConstraint, IMessage> messages = ImmutableMap.builder();
        messages.putAll(result.messages());

        delays.forEach((c, d) -> messages.put(c, new Unsolved(MessageUtil.findClosestMessage(c), d, c.ownCriticalEdges().orElse(null))));

        return result.withMessages(messages.build()).withDelays(ImmutableMap.of());
    }

    private static class Unsolved implements IMessage, Serializable {

        private static final long serialVersionUID = 1L;

        private final IMessage message;
        private final Delay delay;
        private final @Nullable ICompleteness.Immutable completeness;

        private Unsolved(IMessage message, Delay delay, @Nullable ICompleteness.Immutable completeness) {
            this.message = message;
            this.delay = delay;
            this.completeness = completeness;
        }

        @Override public MessageKind kind() {
            return message.kind();
        }

        @Override public String toString(TermFormatter formatter, Function0<String> getDefaultMessage,
                                         Function1<ICompleteness.Immutable, String> formatCompleteness) {
            StringBuilder sb = new StringBuilder("(unsolved)");
            final String msg = message.toString(formatter, getDefaultMessage, formatCompleteness);
            if(!msg.isEmpty()) {
                sb.append(" ");
                sb.append(msg);
                sb.append(":");
            }
            sb.append(" delayed on");
            boolean first = true;
            if(!delay.vars().isEmpty()) {
                sb.append(" vars: ")
                    .append(delay.vars().stream().map(ITermVar::toString).collect(Collectors.joining(", ")));
                first = false;
            }
            if(!delay.criticalEdges().isEmpty()) {
                if(!first) {
                    sb.append(" and");
                }
                sb.append(" critial edges: ").append(
                    delay.criticalEdges().stream().map(CriticalEdge::toString).collect(Collectors.joining(", ")));
                first = false;
            }
            if(completeness != null && !completeness.isEmpty()) {
                if(!first) {
                    sb.append(",");
                }
                sb.append(" preventing completion of ").append(formatCompleteness.apply(completeness));
            }
            return sb.toString();
        }

        @Override public Optional<ITerm> origin() {
            return message.origin();
        }

        @Override public void visitVars(Action1<ITermVar> onVar) {
            message.visitVars(onVar);
        }

        @Override public IMessage apply(ISubstitution.Immutable subst) {
            return new Unsolved(message.apply(subst), delay, completeness == null ? null : completeness.apply(subst));
        }

        @Override public IMessage apply(IRenaming subst) {
            return new Unsolved(message.apply(subst), delay, completeness == null ? null : completeness.apply(subst));
        }

    }


}
