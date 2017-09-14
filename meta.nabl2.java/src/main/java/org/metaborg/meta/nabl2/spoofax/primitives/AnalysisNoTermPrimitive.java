package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.spoofax.analysis.StrategoAnalysis;
import org.metaborg.meta.nabl2.stratego.ConstraintTerms;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public abstract class AnalysisNoTermPrimitive extends ScopeGraphContextPrimitive {

    public AnalysisNoTermPrimitive(String name) {
        super(name, 0, 1);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(sterms.size() != 1) {
            throw new IllegalArgumentException("Expected one term argument: analysis");
        }
        if(!(sterms.get(0) instanceof StrategoAnalysis)) {
            throw new IllegalArgumentException("Not a valid analysis term.");
        }
        final StrategoAnalysis analysis = (StrategoAnalysis) sterms.get(0);
        StrategoTerms strategoTerms = new StrategoTerms(factory);
        Optional<? extends ITerm> result = call(analysis);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    public abstract Optional<? extends ITerm> call(IScopeGraphUnit unit) throws InterpreterException;

}