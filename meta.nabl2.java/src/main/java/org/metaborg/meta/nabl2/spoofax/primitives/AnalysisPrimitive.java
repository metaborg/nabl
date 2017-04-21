package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.ConstraintTerms;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndices;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public abstract class AnalysisPrimitive extends ScopeGraphContextPrimitive {

    public AnalysisPrimitive(String name) {
        super(name, 0, 1);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(sterms.size() != 1) {
            throw new IllegalArgumentException("Expected one term argument: analysis");
        }
        TermIndex index = StrategoTermIndices.get(sterms.get(0))
                .orElseThrow(() -> new IllegalArgumentException("Not a valid analysis term."));
        StrategoTerms strategoTerms = new StrategoTerms(factory);
        ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        Optional<? extends ITerm> result = call(context, index, term);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    public abstract Optional<? extends ITerm> call(IScopeGraphContext<?> context, TermIndex index, ITerm term)
            throws InterpreterException;

}
