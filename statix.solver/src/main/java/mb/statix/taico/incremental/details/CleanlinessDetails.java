package mb.statix.taico.incremental.details;

import java.io.Serializable;
import java.util.Set;

import mb.statix.taico.module.IModule;

public interface CleanlinessDetails extends Serializable {
    /**
     * @return
     *      the modules why this module is potentially dirty
     */
    public Set<IModule> dirtynessDependsOn();
}
