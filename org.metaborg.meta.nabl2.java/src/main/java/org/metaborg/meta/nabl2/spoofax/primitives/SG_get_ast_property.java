package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_ast_property extends ScopeGraphPrimitive {

    public SG_get_ast_property() {
        super(SG_get_ast_property.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        TermIndex index = term.getAttachments().getInstance(TermIndex.class);
        if (index == null) {
            return Optional.empty();
        }
        if (terms.size() != 1) {
            throw new InterpreterException("Need one term argument: key");
        }
        ITerm key = terms.get(0);
        return context.unit(index.getResource()).solution().<ITerm> flatMap(s -> {
            return s.getAstProperties().getValue(index, key).map(s.getUnifier()::find);
        });
    }

}