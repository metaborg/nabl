package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnifierTerms;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_debug_unifier extends AnalysisNoTermPrimitive {

    public SG_debug_unifier() {
        super(SG_debug_unifier.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit) throws InterpreterException {
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(), UnifierTerms.build(sol.unifier()));
        });
    }

}