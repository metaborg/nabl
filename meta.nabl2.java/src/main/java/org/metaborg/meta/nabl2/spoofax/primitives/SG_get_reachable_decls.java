package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.TB;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_get_reachable_decls extends AnalysisPrimitive {

    public SG_get_reachable_decls() {
        super(SG_get_reachable_decls.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().<ITerm>flatMap(s -> {
            return Scope.matcher().match(term, s.unifier()).<ITerm>flatMap(scope -> {
                return s.nameResolution().reachable(scope).map(TB::newList);
            });
        });
    }

}