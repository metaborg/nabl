package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_get_custom_analysis extends AnalysisPrimitive {

    public SG_get_custom_analysis() {
        super(SG_get_custom_analysis.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.customSolution().map(cs -> cs.getAnalysis());
    }

}