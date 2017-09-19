package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.ITermIndex;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_ast_property extends ScopeGraphPrimitive {

    public SG_get_ast_property() {
        super(SG_get_ast_property.class.getSimpleName(), 0, 2);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (terms.size() != 2) {
            throw new InterpreterException("Need two term arguments: analysis, key");
        }
        final ITermIndex analysis = TermIndex.get(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Not an analysis term."));
        final ITerm key = terms.get(1);
        return TermIndex.get(term).flatMap(index -> {
            return context.unit(analysis.getResource()).solution().<ITerm> flatMap(s -> {
                return s.getAstProperties().getValue(index, key).map(s.getUnifier()::find);
            });
        });
    }

}