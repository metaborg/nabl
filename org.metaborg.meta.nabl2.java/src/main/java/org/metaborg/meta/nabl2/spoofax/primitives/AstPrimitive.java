package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.ConstraintTerms;
import org.metaborg.meta.nabl2.stratego.StrategoTermIndices;
import org.metaborg.meta.nabl2.stratego.StrategoTerms;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public abstract class AstPrimitive extends ScopeGraphContextPrimitive {

    public AstPrimitive(String name, int svars, int tvars) {
        super(name, svars, tvars);
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphContext<?> context, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        Optional<TermIndex> maybeIndex = StrategoTermIndices.get(sterm);
        if(!maybeIndex.isPresent()) {
            return Optional.empty();
        }
        TermIndex index = maybeIndex.get();
        StrategoTerms strategoTerms = new StrategoTerms(factory);
        List<ITerm> terms = sterms.stream().map(strategoTerms::fromStratego).map(ConstraintTerms::specialize)
                .collect(Collectors.toList());
        Optional<? extends ITerm> result = call(context, index, terms);
        return result.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    public abstract Optional<? extends ITerm> call(IScopeGraphContext<?> context, TermIndex index, List<ITerm> terms)
            throws InterpreterException;

}
