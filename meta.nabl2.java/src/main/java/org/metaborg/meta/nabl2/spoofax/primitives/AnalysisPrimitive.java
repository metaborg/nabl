package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.spoofax.analysis.StrategoAnalysis;
import org.metaborg.meta.nabl2.stratego.ConstraintTerms;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.NotImplementedException;

public abstract class AnalysisPrimitive extends ScopeGraphContextPrimitive {

    public AnalysisPrimitive(String name) {
        this(name, 0);
    }

    public AnalysisPrimitive(String name, int tvars) {
        super(name, 0, tvars + 1);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        if(sterms.size() < 1) {
            throw new IllegalArgumentException("Expected as first term argument: analysis");
        }
        if(!(sterms.get(0) instanceof StrategoAnalysis)) {
            throw new IllegalArgumentException("Not a valid analysis term.");
        }
        final StrategoAnalysis analysis = (StrategoAnalysis) sterms.get(0);
        return call(analysis, sterm, sterms, factory);
    }

    public Optional<? extends IStrategoTerm> call(IScopeGraphUnit unit, IStrategoTerm sterm, List<IStrategoTerm> sterms,
            ITermFactory factory) throws InterpreterException {
        final StrategoTerms strategoTerms = new StrategoTerms(factory);
        final List<ITerm> terms = sterms.stream().skip(1).map(strategoTerms::fromStratego)
                .map(ConstraintTerms::specialize).collect(Collectors.toList());
        final ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        Optional<? extends ITerm> result = call(unit, term, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    @SuppressWarnings("unused") public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        throw new NotImplementedException("Subclasses should override call method.");
    }

}