package org.metaborg.meta.nabl2.config;

import java.io.Serializable;

public class NaBL2Config implements Serializable {
    private static final long serialVersionUID = 42L;

    public static final NaBL2Config DEFAULT = new NaBL2Config(false, NaBL2DebugConfig.NONE);
    
    private final boolean incremental;
    private final NaBL2DebugConfig debug;

    public NaBL2Config(boolean incremental, NaBL2DebugConfig debug) {
        this.incremental = incremental;
        this.debug = debug;
    }

    public boolean incremental() {
        return incremental;
    }

    public NaBL2DebugConfig debug() {
        return debug;
    }

}
