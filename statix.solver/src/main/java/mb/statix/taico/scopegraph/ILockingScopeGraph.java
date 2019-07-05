package mb.statix.taico.scopegraph;

import java.util.Collection;

import mb.statix.taico.scopegraph.locking.LockManager;

public interface ILockingScopeGraph<S, L, D> extends IMInternalScopeGraph<S, L, D> {
    @Override
    Collection<? extends ILockingScopeGraph<S, L, D>> getChildren();
    
    /**
     * @return
     *      the lock manager
     */
    LockManager getLockManager();
    
    /**
     * Unlocks all locks held on this tracking scope graph.
     */
    default void unlockAll() {
        getLockManager().releaseAll();
    }
}
