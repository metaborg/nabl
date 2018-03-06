package mb.nabl2.spoofax.analysis;

import java.util.Collection;

import org.metaborg.util.concurrent.IClosableLock;

import mb.nabl2.config.NaBL2Config;

public interface IScopeGraphContext<U extends IScopeGraphUnit> {

    IClosableLock guard();

    /** Get unit for the given resource */
    U unit(String resource);

    /** Get all units in this context */
    Collection<U> units();

    NaBL2Config config();
    
}