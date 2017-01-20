package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_decl_property extends ScopeGraphPrimitive {

    public SG_get_decl_property() {
        super(SG_get_decl_property.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (terms.size() != 1) {
            throw new InterpreterException("Need one term argument: key");
        }
        ITerm key = terms.get(0);
        return Occurrence.matcher().match(term).<ITerm> flatMap(decl -> {
            return context.unit(decl.getIndex().getResource()).solution().<ITerm> flatMap(s -> {
                return s.getDeclProperties().getValue(decl, key).map(s.getUnifier()::find);
            });
        });
    }

}