package org.metaborg.meta.nabl2.spoofax.analysis;

import java.util.Collection;

import org.metaborg.meta.nabl2.config.NaBL2Config;
import org.metaborg.util.concurrent.IClosableLock;

public interface IScopeGraphContext<U extends IScopeGraphUnit> {

    IClosableLock guard();

    /** Get unit for the given resource */
    U unit(String resource);

    /** Get all units in this context */
    Collection<U> units();

    NaBL2Config config();
    
}