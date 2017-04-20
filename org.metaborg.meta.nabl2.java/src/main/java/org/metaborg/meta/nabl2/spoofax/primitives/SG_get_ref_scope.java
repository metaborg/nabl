package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_ref_scope extends AnalysisPrimitive {

    public SG_get_ref_scope() {
        super(SG_get_ref_scope.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphContext<?> context, TermIndex index, ITerm term)
            throws InterpreterException {
        return Occurrence.matcher().match(term).<ITerm>flatMap(ref -> {
            return context.unit(index.getResource()).solution().<ITerm>flatMap(s -> {
                return s.getScopeGraph().getRefs().get(ref).flatMap(Optional::<ITerm>of);
            });
        });
    }

}