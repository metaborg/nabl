package mb.statix.spoofax;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.solver.persistent.SolverResult;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import java.util.List;
import java.util.Optional;

import static mb.nabl2.terms.build.TermBuild.B;

public class STX_delays_as_errors extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_delays_as_errors() {
        super(STX_delays_as_errors.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
        throws InterpreterException {

        final IStatixProjectConfig config = getConfig(term);
        final SolverResult<?> result = getResult(term);

        final SolverResult<?> newResult = MessageUtil.delaysAsErrors(result, config.suppressCascadingErrors());
        return Optional.of(B.newBlob(newResult));
    }
}
