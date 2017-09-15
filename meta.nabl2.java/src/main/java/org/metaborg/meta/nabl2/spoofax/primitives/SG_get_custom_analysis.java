package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class SG_get_custom_analysis extends AnalysisNoTermPrimitive {

    public SG_get_custom_analysis() {
        super(SG_get_custom_analysis.class.getSimpleName());
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphUnit unit, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        StrategoTerms strategoTerms = new StrategoTerms(factory);
        return unit.customSolution().map(cs -> cs.getAnalysis()).map(strategoTerms::toStratego);
    }

}