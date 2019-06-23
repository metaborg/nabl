package mb.statix.taico.module;

public enum ModuleCleanliness {
    //NOTE: The constants are defined in a particular ordering, to ensure that the compareTo method
    //      orders them in order of importance.
    
    /** Indicates that the module does no longer exist. */
    DELETED,
    
    /** One of the children of the module were removed. */
    REMOVEDCHILD,
    
    /** Indicates that the module is new. */
    NEW,
    
    /** Indicates that the module has a new child that potentially affects the rest. */
    NEWCHILD,
    
    /** Indicates that the module is guaranteed to be dirty. */
    DIRTY,
    
    /** Indicates that the module is a child of a dirty module. */
    CHILDOFDIRTY,
    
    /**
     * Indicates that the module has a dirty child.
     * <p>
     * A module <i>A</i> is flagged as dirtyChild, level 1, if there is a descendant
     * <i>B</i> of <i>A</i> which is marked dirty and is passed a scope owned by <i>A</i>.
     */
    DIRTYCHILD,
    
    /** Indicates that we are unsure if the module is dirty or not. */
    CLIRTY,
    
    /** Indicates that the module has a clirty child. */
    CLIRTYCHILD,
    
    /** Indicates that the module is guaranteed to be clean. */
    CLEAN;
    
    public boolean isDirtyish() {
        return !isCleanish();
    }
    
    public boolean isCleanish() {
        return this == CLEAN || this == NEW;
    }
}
