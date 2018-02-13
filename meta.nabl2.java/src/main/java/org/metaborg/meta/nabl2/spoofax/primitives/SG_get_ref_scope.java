package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_ref_scope extends AnalysisPrimitive {

    public SG_get_ref_scope() {
        super(SG_get_ref_scope.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().<ITerm>flatMap(s -> {
            return Occurrence.matcher().match(term, s.unifier()).<ITerm>flatMap(ref -> {
                return s.scopeGraph().getRefs().get(ref).flatMap(Optional::<ITerm>of);
            });
        });
    }

}