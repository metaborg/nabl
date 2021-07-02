package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.SolverResult;

public class STX_delays_as_errors extends StatixPrimitive {

    @Inject public STX_delays_as_errors() {
        super(STX_delays_as_errors.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final SolverResult result = M.blobValue(SolverResult.class).match(term)
                .orElseThrow(() -> new InterpreterException("Expected solver result."));

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

        // Collect all delays that involve variables that do not occur on free variables.
        ImmutableMap<IConstraint, Delay> delays = result.delays();

        while(!newErrorVars.isEmpty()) {
            final Set.Transient<ITermVar> _newVars = CapsuleUtil.transientSet();
            final ImmutableMap.Builder<IConstraint, Delay> retainedDelays = ImmutableMap.builder();

            for(Entry<IConstraint, Delay> e : delays.entrySet()) {
                if(e.getValue().vars().stream().anyMatch(newErrorVars::contains)) {
                    for(ITermVar var : e.getKey().freeVars()) {
                        _newVars.__insertAll(unifier.findRecursive(var).getVars().__removeAll(allErrorVars));
                    }
                } else {
                    retainedDelays.put(e);
                }
            }

            delays = retainedDelays.build();
            newErrorVars = _newVars.freeze();
            allErrorVars = allErrorVars.__insertAll(newErrorVars);
        }

        final ImmutableMap.Builder<IConstraint, IMessage> messages = ImmutableMap.builder();
        messages.putAll(result.messages());

        delays.forEach((c, d) -> {
            messages.put(c, new Unsolved(MessageUtil.findClosestMessage(c), d, c.ownCriticalEdges().orElse(null)));
        });

        final SolverResult newResult = result.withMessages(messages.build()).withDelays(ImmutableMap.of());
        return Optional.of(B.newBlob(newResult));
    }

    private class Unsolved implements IMessage, Serializable {

        private static final long serialVersionUID = 1L;

        private final IMessage message;
        private final Delay delay;
        private final @Nullable ICompleteness.Immutable completeness;

        private Unsolved(IMessage message, Delay delay, ICompleteness.Immutable completeness) {
            this.message = message;
            this.delay = delay;
            this.completeness = completeness;
        }

        @Override public MessageKind kind() {
            return message.kind();
        }

        @Override public String toString(TermFormatter formatter, Function0<String> getDefaultMessage) {
            StringBuilder sb = new StringBuilder("(unsolved)");
            final String msg = message.toString(formatter, getDefaultMessage);
            if(!msg.isEmpty()) {
                sb.append(" ");
                sb.append(msg);
                sb.append(" ");
            }
            sb.append(" delayed on: ");
            if(!delay.vars().isEmpty()) {
                sb.append("vars ").append(delay.vars());
            }
            if(!delay.criticalEdges().isEmpty()) {
                if(!delay.vars().isEmpty()) {
                    sb.append(" and ");
                }
                sb.append("critial edges ").append(delay.criticalEdges());
            }
            if(completeness != null && !completeness.isEmpty()) {
                sb.append(" preventing completion of ").append(completeness);
            }
            return sb.toString();
        }

        @Override public Optional<ITerm> origin() {
            return message.origin();
        }

        @Override public void visitVars(Action1<ITermVar> onVar) {
            message.visitVars(onVar);
        }

        @Override public IMessage apply(Immutable subst) {
            return new Unsolved(message.apply(subst), delay, completeness == null ? null : completeness.apply(subst));
        }

        @Override public IMessage apply(IRenaming subst) {
            return new Unsolved(message.apply(subst), delay, completeness == null ? null : completeness.apply(subst));
        }

    }

}