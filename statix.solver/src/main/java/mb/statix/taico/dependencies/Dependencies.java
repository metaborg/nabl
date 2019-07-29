package mb.statix.taico.dependencies;

import java.io.Serializable;
import java.util.Set;

import mb.statix.taico.module.IModule;

public abstract class Dependencies implements Serializable {
    private static final long serialVersionUID = 1L;

    public abstract Set<IModule> getDependencies();
    
}
