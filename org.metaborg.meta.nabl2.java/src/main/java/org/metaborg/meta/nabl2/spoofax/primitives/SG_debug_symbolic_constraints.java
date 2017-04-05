package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.SymbolicTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_debug_symbolic_constraints extends ScopeGraphPrimitive {

    public SG_debug_symbolic_constraints() {
        super(SG_debug_symbolic_constraints.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        return TermIndex.get(terms.get(0)).flatMap(index -> {
            final IScopeGraphUnit unit = context.unit(index.getResource());
            return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
                return TermSimplifier.focus(unit.resource(), SymbolicTerms.build(sol.getSymbolic(), sol.getUnifier()));
            });
        });
    }

}