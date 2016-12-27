package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_custom_analysis extends ScopeGraphPrimitive {

    public SG_get_custom_analysis() {
        super(SG_get_custom_analysis.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        TermIndex index = term.getAttachments().getInstance(TermIndex.class);
        if (index == null) {
            return Optional.empty();
        }
        return context.unit(index.getResource()).customSolution().<ITerm> map(cs -> cs.getAnalysis());
    }

}