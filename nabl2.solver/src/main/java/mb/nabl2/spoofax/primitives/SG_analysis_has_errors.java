package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class SG_analysis_has_errors extends AnalysisPrimitive {

    public SG_analysis_has_errors() {
        super(SG_analysis_has_errors.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return solution.messages().getErrors().isEmpty() ? Optional.empty() : Optional.of(term);
    }

}