package mb.statix.taico.util;

import mb.statix.taico.module.IModule;

/**
 * Interface for objects that can be owned by a module.
 */
public interface IOwnable {
    /**
     * @return
     *      the owner of this item
     */
    IModule getOwner();
}
