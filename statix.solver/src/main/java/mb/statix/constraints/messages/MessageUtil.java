package mb.statix.constraints.messages;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.stream.StreamUtil;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.Constraints;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.tracer.SolverTracer;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import mb.statix.spoofax.IStatixProjectConfig;

import static mb.nabl2.terms.build.TermBuild.B;

public class MessageUtil {

    // @formatter:off
    private static final Map<Class<? extends IConstraint>, MessageKind> KINDS =
        io.usethesource.capsule.Map.Immutable.of(
            CAstId.class, MessageKind.IGNORE,
            CAstProperty.class, MessageKind.IGNORE);
    // @formatter:on

    public static IMessage findClosestMessage(IConstraint c) {
        return findClosestMessage(c, KINDS.getOrDefault(c.getClass(), null));
    }

    /**
     * Find closest message in the
     */
    public static IMessage findClosestMessage(IConstraint c, @Nullable MessageKind kind) {
        @Nullable IMessage message = null;
        while(c != null) {
            @Nullable IMessage m;
            if((m = c.message().orElse(null)) != null && (message == null || message.kind().isWorseThan(m.kind()))) {
                message = m;
            }
            c = c.cause().orElse(null);
        }
        if(message == null) {
            if(kind == null) {
                message = new Message(MessageKind.ERROR);
            } else {
                message = new Message(kind);
            }
        } else if(kind != null) {
            message = message.withKind(kind);
        }
        return message;
    }

    public static <TR extends SolverTracer.IResult<TR>> SolverResult<TR> delaysAsErrors(SolverResult<TR> result, boolean suppressCascadingErrors) {

        io.usethesource.capsule.Map.Immutable<IConstraint, Delay> delays = result.delays();

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
                final io.usethesource.capsule.Map.Transient<IConstraint, Delay> retainedDelays = CapsuleUtil.transientMap();

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
                                    .<CriticalEdge>map(edge -> CriticalEdge.of(scope, edge)).collect(CapsuleCollectors.toSet());
                            _newCriticalEdges.__insertAll(edges.__removeAll(allCriticalEdges));
                        }
                    } else {
                        retainedDelays.__put(e.getKey(), e.getValue());
                    }
                }

                delays = retainedDelays.freeze();
                newErrorVars = _newVars.freeze();
                allErrorVars = allErrorVars.__insertAll(newErrorVars);

                newCriticalEdges = _newCriticalEdges.freeze();
                allCriticalEdges = allCriticalEdges.__insertAll(newCriticalEdges);
            }
        }

        final io.usethesource.capsule.Map.Transient<IConstraint, IMessage> messages = CapsuleUtil.transientMap();
        messages.__putAll(result.messages());

        delays.forEach((c, d) -> messages.__put(c,
                new Unsolved(MessageUtil.findClosestMessage(c), d, c.ownCriticalEdges().orElse(null))));

        return result.withMessages(messages.freeze()).withDelays(CapsuleUtil.immutableMap());
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

        @Override public IMessage withKind(MessageKind kind) {
            return new Unsolved(message.withKind(kind), delay, completeness);
        }

    }



    ////////////////////////////////////////////////
    // Helper methods for creating error messages //
    ////////////////////////////////////////////////

    public static void addMessage(final IMessage message, final IConstraint constraint, final IUniDisunifier unifier,
            IStatixProjectConfig config, final Collection<ITerm> errors, final Collection<ITerm> warnings,
            final Collection<ITerm> notes) {
        Tuple2<Collection<String>, ITerm> message_origin = formatMessage(message, constraint, unifier, config);

        final String messageText = message_origin._1().stream().filter(s -> !s.isEmpty())
                .map(s -> cleanupString(s)).collect(Collectors.joining("<br>\n&gt;&nbsp;", "", "<br>\n"));

        final ITerm messageTerm = B.newTuple(message_origin._2(), B.newString(messageText));
        switch(message.kind()) {
            case ERROR:
                errors.add(messageTerm);
                break;
            case WARNING:
                warnings.add(messageTerm);
                break;
            case NOTE:
                notes.add(messageTerm);
                break;
            case IGNORE:
                break;
        }

    }

    public static Tuple2<Collection<String>, ITerm> formatMessage(final IMessage message, final IConstraint constraint,
            final IUniDisunifier unifier, IStatixProjectConfig config) {
        final TermFormatter formatter = Solver.shallowTermFormatter(unifier,
                config.messageTermDepth(config.messageTermDepth(IStatixProjectConfig.DEFAULT_MESSAGE_TERM_DEPTH)));
        final int maxTraceLength =
                config.messageTraceLength(config.messageTraceLength(IStatixProjectConfig.DEFAULT_MESSAGE_TRACE_LENGTH));

        ITerm originTerm =
                message.origin().flatMap(t -> getOriginTerm(t, unifier)).orElse(findOrigin(constraint, unifier));
        final Deque<String> trace = formatTrace(constraint, unifier, formatter, maxTraceLength);

        // add constraint message
        trace.addFirst(message.toString(formatter, () -> constraint.toString(formatter), completeness -> {
            final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
            completeness.vars().forEach(var -> {
                ITerm sub = unifier.findRecursive(var);
                if(!sub.equals(var)) {
                    subst.put(var, sub);
                }
            });
            return completeness.apply(subst.freeze()).entrySet().stream().flatMap(e -> {
                String scope = e.getKey().toString();
                return e.getValue().elementSet().stream().map(edge -> scope + "-" + edge.toString());
            }).collect(Collectors.joining(", "));
        }));

        // use empty origin if none was found
        if(originTerm == null) {
            originTerm = B.newTuple();
        }

        return Tuple2.of(trace, originTerm);
    }

    private static ITerm findOrigin(IConstraint constraint, final IUniDisunifier unifier) {
        while(constraint != null) {
            final ITerm origin = findOriginArgument(constraint, unifier).orElse(null);
            if(origin != null) {
                return origin;
            }
            constraint = constraint.cause().orElse(null);
        }
        return null;
    }

    public static Deque<String> formatTrace(final IConstraint constraint, final IUniDisunifier unifier,
            final TermFormatter formatter, final int maxTraceLength) {
        final Deque<String> trace = new ArrayDeque<>();
        IConstraint current = constraint;
        int traceCount = 0;
        while(current != null) {
            if(maxTraceLength < 0 || ++traceCount <= maxTraceLength) {
                trace.addLast(current.toString(formatter));
            }
            current = current.cause().orElse(null);
        }
        if(maxTraceLength > 0 && traceCount > maxTraceLength) {
            trace.addLast("... trace truncated ...");
        }
        return trace;
    }

    private static Optional<ITerm> findOriginArgument(IConstraint constraint, IUniDisunifier unifier) {
        // @formatter:off
        final Function1<IConstraint, Stream<ITerm>> terms = Constraints.cases(
            onArith -> Stream.empty(),
            onConj -> Stream.empty(),
            onEqual -> Stream.empty(),
            onExists -> Stream.empty(),
            onFalse -> Stream.empty(),
            onInequal -> Stream.empty(),
            onNew -> Stream.empty(),
            onResolveQuery -> Stream.empty(),
            onTellEdge -> Stream.empty(),
            onTermId -> Stream.empty(),
            onTermProperty -> Stream.empty(),
            onTrue -> Stream.empty(),
            onTry -> Stream.empty(),
            onUser -> onUser.args().stream()
        );
        // @formatter:on
        return StreamUtil.filterMap(terms.apply(constraint), t -> getOriginTerm(t, unifier)).findFirst();
    }

    private static Optional<ITerm> getOriginTerm(ITerm term, IUniDisunifier unifier) {
        // @formatter:off
        return Optional.of(unifier.findTerm(term))
            .filter(t -> TermIndex.get(t).isPresent())
            .filter(t -> TermOrigin.get(t).isPresent()) // HACK Ignore terms without origin, such as empty lists
            .map(t -> B.newTuple(ImList.Immutable.of(), t.getAttachments()));
        // @formatter:on
    }

    private static String cleanupString(String string) {
        return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r\n", "<br>")
                .replace("\n", "<br>").replace("\r", "<br>").replace("\t", "&Tab;");
    }
}
