package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.util.log.Level;

public class SG_log_info extends ScopeGraphLogPrimitive {

    public SG_log_info() {
        super(SG_log_info.class.getSimpleName());
    }

    @Override protected Level level(IScopeGraphContext<?> context) {
        return Level.Info;
    }

}