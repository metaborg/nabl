package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.statix.solver.persistent.SolverResult;

public class STX_is_analysis extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_is_analysis() {
        super(STX_is_analysis.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return M.blobValue(SolverResult.class).map(B::newBlob).match(term);
    }

}