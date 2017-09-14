package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_ast_property extends AnalysisPrimitive {

    public SG_get_ast_property() {
        super(SG_get_ast_property.class.getSimpleName(), 1);
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(terms.size() != 1) {
            throw new InterpreterException("Need one term argument: key");
        }
        final ITerm key = terms.get(0);
        return TermIndex.get(term).<ITerm>flatMap(index -> {
            return unit.solution().<ITerm>flatMap(s -> {
                return s.astProperties().getValue(index, key).map(s.unifier()::find);
            });
        });
    }

}