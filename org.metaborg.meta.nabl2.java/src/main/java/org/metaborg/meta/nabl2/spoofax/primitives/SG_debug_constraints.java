package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_debug_constraints extends AnalysisNoTermPrimitive {

    public SG_debug_constraints() {
        super(SG_debug_constraints.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphContext<?> context, TermIndex index)
            throws InterpreterException {
        IScopeGraphUnit unit = context.unit(index.getResource());
        return Optional.of(TermSimplifier.focus(unit.resource(), Constraints.build(unit.constraints())));
    }

}