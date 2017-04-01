package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.util.log.Level;

public class SG_log_error extends ScopeGraphLogPrimitive {

    public SG_log_error() {
        super(SG_log_error.class.getSimpleName());
    }

    @Override protected Level level(IScopeGraphContext<?> context) {
        return Level.Error;
    }

}