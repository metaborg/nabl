package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.spoofax.analysis.IScopeGraphUnit;

public class SG_analysis_has_errors extends AnalysisPrimitive {

    public SG_analysis_has_errors() {
        super(SG_analysis_has_errors.class.getSimpleName());
    }

    @Override protected Optional<? extends IStrategoTerm> call(IScopeGraphUnit unit, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        return unit.solution().filter(s -> !s.messages().getErrors().isEmpty()).map(s -> sterm);
    }

}