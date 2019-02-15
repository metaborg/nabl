package mb.statix.taico.util;

import mb.statix.taico.module.IModule;

/**
 * Interface for objects that can be owned by a module.
 */
public interface IOwnable<V extends IOwnable<V, L, R>, L, R> {
    /**
     * @return
     *      the owner of this item
     */
    IModule<V, L, R> getOwner();
}
