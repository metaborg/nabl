package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.NotImplementedException;

import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.terms.ITerm;

public abstract class AnalysisNoTermPrimitive extends AnalysisPrimitive {

    public AnalysisNoTermPrimitive(String name) {
        super(name, 0);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphUnit unit, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        StrategoTerms strategoTerms = new StrategoTerms(factory);
        Optional<? extends ITerm> result = call(unit);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    @SuppressWarnings("unused") public Optional<? extends ITerm> call(IScopeGraphUnit unit)
            throws InterpreterException {
        throw new NotImplementedException("Subclasses should override call method.");
    }

}