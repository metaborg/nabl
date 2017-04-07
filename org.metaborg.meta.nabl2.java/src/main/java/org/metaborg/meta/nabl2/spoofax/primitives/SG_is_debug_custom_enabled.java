package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_is_debug_custom_enabled extends ScopeGraphPrimitive {

    public SG_is_debug_custom_enabled() {
        super(SG_is_debug_custom_enabled.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return context.config().debug().custom() ? Optional.of(term) : Optional.empty();
    }

}