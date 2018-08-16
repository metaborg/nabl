package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.spoofax.analysis.IResult;
import mb.nabl2.terms.ITerm;

public class SG_set_custom_analysis extends AnalysisPrimitive {

    public SG_set_custom_analysis() {
        super(SG_set_custom_analysis.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IResult result, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Optional.of(B.newBlob(result.withCustomAnalysis(term)));
    }

}