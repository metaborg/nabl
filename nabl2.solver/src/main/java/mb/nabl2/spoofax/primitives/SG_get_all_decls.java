package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class SG_get_all_decls extends AnalysisPrimitive {

    public SG_get_all_decls() {
        super(SG_get_all_decls.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Optional.of(B.newList(solution.scopeGraph().getAllDecls()));
    }

}