package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
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
        final ImmutableMap.Builder<IConstraint, IMessage> messages = ImmutableMap.builder();
        messages.putAll(result.messages());
        result.delays().keySet().forEach(c -> {
            messages.put(c, new Unsolved(MessageUtil.findClosestMessage(c)));
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

        @Override public String toString(TermFormatter formatter) {
            final String msg = message.toString(formatter);
            return msg + (msg.isEmpty() ? "" : " ") + "(unsolved)";
        }

        @Override public Optional<ITerm> origin() {
            return message.origin();
        }

        @Override public IMessage apply(Immutable subst) {
            return new Unsolved(message.apply(subst));
        }

        @Override public IMessage apply(IRenaming subst) {
            return new Unsolved(message.apply(subst));
        }

    }

}