package mb.statix.modular.util;

import mb.statix.modular.module.IModule;

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
