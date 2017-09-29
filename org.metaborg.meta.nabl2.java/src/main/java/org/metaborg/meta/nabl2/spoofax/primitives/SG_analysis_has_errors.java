package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.ITermIndex;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_analysis_has_errors extends ScopeGraphPrimitive {

    public SG_analysis_has_errors() {
        super(SG_analysis_has_errors.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        final ITermIndex analysis = TermIndex.get(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Not an analysis term."));
        return TermIndex.get(term).flatMap(index -> {
            return context.unit(analysis.getResource()).solution().flatMap(s -> {
                if(s.getMessages().getErrors().isEmpty()) {
                    return Optional.empty();
                } else {
                    return Optional.of(term);
                }
            });
        });
    }

}