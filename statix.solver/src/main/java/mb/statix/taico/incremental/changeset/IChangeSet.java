package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Set;

import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;

/**
 * Implementations have to support at least
 * {@link ModuleCleanliness#DELETED},
 * {@link ModuleCleanliness#CLEAN},
 * {@link ModuleCleanliness#NEW} (added),
 * {@link ModuleCleanliness#DIRTY}
 */
public interface IChangeSet extends Serializable {
    /**
     * @return
     *      the enum map storing the modules for the different cleanlinesses
     */
    EnumMap<ModuleCleanliness, Set<IModule>> cleanlinessToModule();

    /**
     * NOTE: This should not be computed, but should at most be stored.
     * 
     * @return
     *      the enum map storing the module ids for the different cleanlinesses
     */
    EnumMap<ModuleCleanliness, Set<String>> cleanlinessToId();

    // --------------------------------------------------------------------------------------------
    // Modules
    // --------------------------------------------------------------------------------------------

    default Set<IModule> removed() {
        return getModules(DELETED);
    }

    default Set<IModule> dirty() {
        return getModules(DIRTY);
    }

    default Set<IModule> clean() {
        return getModules(CLEAN);
    }

    // Potentially unsupported
    default Set<IModule> unsure() throws UnsupportedOperationException {
        return getModules(UNSURE);
    }

    default Set<IModule> hasDirtyChild() throws UnsupportedOperationException {
        return getModules(DIRTYCHILD);
    }
    
    default Set<IModule> hasUnsureChild() throws UnsupportedOperationException {
        return getModules(UNSURECHILD);
    }
    
    default Set<IModule> childOfDirty() throws UnsupportedOperationException {
        return getModules(CHILDOFDIRTY);
    }
    
    default Set<IModule> hasNewChild() throws UnsupportedOperationException {
        return getModules(NEWCHILD);
    }

    // --------------------------------------------------------------------------------------------
    // Ids
    // --------------------------------------------------------------------------------------------

    default Set<String> added() {
        return getIds(NEW);
    }

    default Set<String> removedIds() {
        return getIds(DELETED);
    }

    default Set<String> dirtyIds() {
        return getIds(DIRTY);
    }

    default Set<String> cleanIds() {
        return getIds(CLEAN);
    }

    // Potentially unsupported
    default Set<String> unsureIds() throws UnsupportedOperationException {
        return getIds(UNSURE);
    }

    default Set<String> hasDirtyChildIds() throws UnsupportedOperationException {
        return getIds(DIRTYCHILD);
    }

    default Set<String> hasUnsureChildIds() throws UnsupportedOperationException {
        return getIds(UNSURECHILD);
    }
    
    default Set<String> hasNewChildIds() throws UnsupportedOperationException {
        return getIds(NEWCHILD);
    }
    
    /**
     * @param cleanliness
     *      the cleanliness 
     * 
     * @return
     *      the modules for the given cleanliness
     * 
     * @throws UnsupportedOperationException
     *      If the given cleanliness is not tracked by this change set.
     */
    default Set<IModule> getModules(ModuleCleanliness cleanliness) throws UnsupportedOperationException {
        Set<IModule> modules = cleanlinessToModule().get(cleanliness);
        if (modules == null) throw new UnsupportedOperationException("This change set does not support " + cleanliness);
        return modules;
    }

    /**
     * @param cleanliness
     *      the cleanliness 
     * 
     * @return
     *      the module ids for the given cleanliness
     * 
     * @throws UnsupportedOperationException
     *      If the given cleanliness is not tracked by this change set.
     */
    default Set<String> getIds(ModuleCleanliness cleanliness) throws UnsupportedOperationException {
        Set<String> modules = cleanlinessToId().get(cleanliness);
        if (modules == null) throw new UnsupportedOperationException("This change set does not support " + cleanliness);
        return modules;
    }
}
