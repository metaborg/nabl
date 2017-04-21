package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.NameResolutionTerms;
import org.metaborg.meta.nabl2.spoofax.TermSimplifier;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_debug_name_resolution extends AnalysisNoTermPrimitive {

    public SG_debug_name_resolution() {
        super(SG_debug_name_resolution.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphContext<?> context, TermIndex index)
            throws InterpreterException {
        final IScopeGraphUnit unit = context.unit(index.getResource());
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(), NameResolutionTerms.build(sol.getNameResolution()));
        });
    }

}