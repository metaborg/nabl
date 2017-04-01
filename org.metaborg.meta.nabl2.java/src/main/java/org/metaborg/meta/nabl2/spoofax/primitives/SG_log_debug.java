package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.util.log.Level;

public class SG_log_debug extends ScopeGraphLogPrimitive {

    public SG_log_debug() {
        super(SG_log_debug.class.getSimpleName());
    }

    @Override protected Level level(IScopeGraphContext<?> context) {
        return context.debug() ? Level.Info : Level.Debug;
    }

}