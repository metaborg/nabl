package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.ScopeGraphTerms;
import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_debug_scope_graph extends AnalysisNoTermPrimitive {

    public SG_debug_scope_graph() {
        super(SG_debug_scope_graph.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphContext<?> context, TermIndex index)
            throws InterpreterException {
        final IScopeGraphUnit unit = context.unit(index.getResource());
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(),
                    ScopeGraphTerms.build(sol.getScopeGraph(), sol.getDeclProperties(), sol.getUnifier()));
        });
    }

}