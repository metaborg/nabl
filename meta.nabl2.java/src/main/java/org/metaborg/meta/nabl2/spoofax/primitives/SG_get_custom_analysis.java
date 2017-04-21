package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndices;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class SG_get_custom_analysis extends ScopeGraphContextPrimitive {

    public SG_get_custom_analysis() {
        super(SG_get_custom_analysis.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        return StrategoTermIndices.get(sterm).flatMap(index -> {
            StrategoTerms strategoTerms = new StrategoTerms(factory);
            return context.unit(index.getResource()).customSolution().map(cs -> cs.getAnalysis())
                    .map(strategoTerms::toStratego);
        });
    }

}