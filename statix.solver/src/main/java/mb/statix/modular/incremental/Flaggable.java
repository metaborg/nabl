package mb.statix.modular.incremental;

import static mb.statix.modular.module.ModuleCleanliness.*;

import mb.statix.modular.module.ModuleCleanliness;
import mb.statix.util.collection.StablePriorityQueue;

public interface Flaggable {
    /**
     * @return
     *      the priority queue with the flags of this {@link Flaggable}
     */
    StablePriorityQueue<Flag> getFlags();
    
    /**
     * @return
     *      the most important flag of this {@link Flaggable}
     */
    default Flag getTopFlag() {
        Flag flag = getFlags().peek();
        return flag == null ? Flag.CLEAN : flag;
    }
    
    default ModuleCleanliness getTopCleanliness() {
        return getTopFlag().getCleanliness();
    }
    
    default int getTopLevel() {
        return getTopFlag().getLevel();
    }
    
    /**
     * Adds the given flag to this {@link Flaggable}.
     * 
     * @param cleanliness
     *      the cleanliness
     * @param level
     *      the level
     */
    default void addFlag(Flag flag) {
        if (flag.equals(Flag.CLEAN)) return;
        getFlags().add(flag);
    }
    
    /**
     * Adds the given flag if there is not already a flag with the same cause as the given flag.
     * 
     * @param flag
     *      the flag to add
     * 
     * @return
     *      true if the flag was added, false otherwise
     * 
     * @throws NullPointerException
     *      If the given flag does not have a cause.
     */
    default boolean addFlagIfNotSameCause(Flag flag) {
        final String cause = flag.getCause();
        if (cause == null) throw new NullPointerException("addFlagIfNotSameCause needs a flag with a non-null cause.");
        
        if (getFlags().stream().anyMatch(other -> cause.equals(other.getCause()))) return false;
        
        getFlags().add(flag);
        return true;
    }
    
    /**
     * Overrides all previous flags with the given flag.
     * 
     * @param flag
     *      the flag
     */
    default void setFlag(Flag flag) {
        getFlags().clear();
        if (flag != Flag.CLEAN) addFlag(flag);
    }
    
    /**
     * Adds the given flag to this {@link Flaggable} if we are currently still clean (have no other flags).
     * 
     * @param flag
     *      the flag
     * 
     * @return
     *      true if the flag was added, false otherwise
     */
    default boolean setFlagIfClean(Flag flag) {
        if (getTopFlag().getCleanliness() != CLEAN) return false;
        addFlag(flag);
        return true;
    }

    /**
     * @return
     *      true if the top flag was removed, false if this module is clean
     */
    default boolean removeTopFlag() {
        return getFlags().poll() != null;
    }
    
    /**
     * Removes the given flag from the flags of this module.
     * 
     * @param flag
     *      the flag to remove
     * @return 
     * 
     * @return
     *      true if the flag was removed, false otherwise
     */
    default boolean removeFlag(Flag flag) {
        return getFlags().remove(flag);
    }
}
