package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.solver.IConstraint;
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
        final Set.Immutable<ITermVar> errorVars = result.messages().entrySet().stream()
            .filter(e -> e.getValue().kind().equals(MessageKind.ERROR))
            .map(Map.Entry::getKey)
            .flatMap(c -> c.freeVars().stream())
            .flatMap(v -> result.state().unifier().findRecursive(v).getVars().stream())
            .collect(CapsuleCollectors.toSet());
        // @formatter:on

        // Collect all delays that involve variables that do not occur on free variables.
        final ImmutableMap.Builder<IConstraint, IMessage> messages = ImmutableMap.builder();
        messages.putAll(result.messages());
        result.delays().entrySet().forEach(e -> {
            if(e.getValue().vars().stream().noneMatch(errorVars::contains)) {
                final IConstraint c = e.getKey();
                messages.put(c, new Unsolved(MessageUtil.findClosestMessage(c)));
            } else {
                logger.debug("Ignoring delay {} because there is already an error on the delayed variables.", e);
            }
        });
        final SolverResult newResult = result.withMessages(messages.build()).withDelays(ImmutableMap.of());
        return Optional.of(B.newBlob(newResult));
    }

    private class Unsolved implements IMessage, Serializable {

        private static final long serialVersionUID = 1L;

        private final IMessage message;

        private Unsolved(IMessage message) {
            this.message = message;
        }

        @Override public MessageKind kind() {
            return message.kind();
        }

        @Override public String toString(TermFormatter formatter, Function0<String> getDefaultMessage) {
            final String msg = message.toString(formatter, getDefaultMessage);
            return "(unsolved)" + (msg.isEmpty() ? "" : " ") + msg;
        }

        @Override public Optional<ITerm> origin() {
            return message.origin();
        }

        @Override public void visitVars(Action1<ITermVar> onVar) {
            message.visitVars(onVar);
        }

        @Override public IMessage apply(Immutable subst) {
            return new Unsolved(message.apply(subst));
        }

        @Override public IMessage apply(IRenaming subst) {
            return new Unsolved(message.apply(subst));
        }

    }

}