package mb.statix.taico.incremental;

import java.util.Set;
import java.util.stream.Collectors;

import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;

public class ChangeSet implements IChangeSet {
    private Set<IModule> removed;
    private Set<IModule> changed;
    
    public ChangeSet(ModuleManager manager, Set<String> removed, Set<String> changed) {
        this.removed = removed.stream().map(manager::getModule).collect(Collectors.toSet());
        this.changed = changed.stream().map(manager::getModule).collect(Collectors.toSet());
    }
    
    @Override
    public Set<IModule> removed() {
        return removed;
    }

    @Override
    public Set<IModule> changed() {
        return changed;
    }

}
