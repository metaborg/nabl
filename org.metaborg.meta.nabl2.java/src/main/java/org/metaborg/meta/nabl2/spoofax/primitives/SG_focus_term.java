package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_focus_term extends ScopeGraphPrimitive {

    public SG_focus_term() {
        super(SG_focus_term.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        return Optional.of(TermIndex.get(terms.get(0)).map(index -> {
            final IScopeGraphUnit unit = context.unit(index.getResource());
            return TermSimplifier.focus(unit.resource(), term);
        }).orElse(term));
    }

}