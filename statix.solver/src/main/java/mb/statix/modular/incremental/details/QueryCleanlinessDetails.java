package mb.statix.modular.incremental.details;

import static mb.statix.modular.module.ModuleCleanliness.*;

import java.util.Set;

import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModuleCleanliness;

public class QueryCleanlinessDetails implements CleanlinessDetails {
    private static final long serialVersionUID = 1L;
    private ModuleCleanliness cleanliness;
    private Set<IModule> dirtyDepends;
    
    public QueryCleanlinessDetails(ModuleCleanliness cleanliness, Set<IModule> dirtyDepends) {
        this.cleanliness = cleanliness;
        this.dirtyDepends = dirtyDepends;
    }
    
    /**
     * @return
     *      true if this module is definitely clean
     */
    public boolean isClean() {
        return cleanliness == CLEAN;
    }
    
    /**
     * @return
     *      true if this module is definitely dirty (but might be unchanged)
     */
    public boolean isDirty() {
        return cleanliness == DIRTY;
    }
    
    /**
     * @return
     *      true if this module might have changed
     */
    public boolean isPotentiallyDirty() {
        switch (cleanliness) {
            case UNSURE:
            case UNSURECHILD:
            case CHILDOFDIRTY:
            case DIRTYCHILD:
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public Set<IModule> dirtynessDependsOn() {
        return dirtyDepends;
    }
    
    public ModuleCleanliness dirtynessDependsOnReason() {
        switch (cleanliness) {
            case UNSURE:
                //Eventually
                return DIRTY;
            case DIRTYCHILD:
                return DIRTY;
            case UNSURECHILD:
                return UNSURE;
            case DIRTY:
                return DIRTY;
            default:
                return null;
        }
    }
}
