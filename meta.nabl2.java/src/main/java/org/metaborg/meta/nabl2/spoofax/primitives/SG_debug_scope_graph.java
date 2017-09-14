package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.ScopeGraphTerms;
import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_debug_scope_graph extends AnalysisNoTermPrimitive {

    public SG_debug_scope_graph() {
        super(SG_debug_scope_graph.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit) throws InterpreterException {
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(),
                    ScopeGraphTerms.build(sol.scopeGraph(), sol.declProperties(), sol.unifier()));
        });
    }

}