package mb.statix.taico.incremental;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.module.ModulePaths;

public class ChangeSet implements IChangeSet {
    private Set<IModule> all;
    private Set<IModule> removed, changed, unchanged;
    private Set<IModule> dirty, clirty, clean;
    private Set<String> added;

    public ChangeSet(ModuleManager manager, Collection<String> removed, Collection<String> changed) {
        this(manager, removed, changed, new HashSet<>());
    }
    
    public ChangeSet(ModuleManager manager, Collection<String> removed, Collection<String> changed, Collection<String> added) {
        System.err.println("All modules in the manager: " + manager.getModules());
        
        this.removed = removed.stream().map(name -> getModule(manager, name)).collect(Collectors.toSet());
        this.changed = changed.stream().map(name -> getModule(manager, name)).collect(Collectors.toSet());
        this.added = new HashSet<>(added);
        
        init(manager);
        validate();
    }
    
    private IModule getModule(ModuleManager manager, String name) {
        IModule module = manager.getModuleByName(name);
        if (module == null) throw new IllegalStateException("Encountered module that is unknown: " + name);
        return module;
    }

    private void init(ModuleManager manager) {
        //Mark the initial status of all modules
        all = new HashSet<>(manager.getModules());
        all.forEach(m -> m.flag(ModuleCleanliness.CLEAN));
        
        //Mark all removed modules and descendants as deleted
        removed.forEach(m -> m.flag(ModuleCleanliness.DELETED));
        removed.stream().flatMap(m -> m.getDescendants()).forEach(m -> m.flag(ModuleCleanliness.DELETED));

        //#0 Compute unchanged = all - removed - changed
        unchanged = new HashSet<>(all);
        unchanged.removeAll(removed);
        unchanged.removeAll(changed);

        //#1 Compute dirty = changed U dependsOn(removed)
        dirty = new HashSet<>();
        for (IModule module : changed) {
            module.flag(ModuleCleanliness.DIRTY);
            dirty.add(module);
        }
        removed.stream().flatMap(m -> m.getDependants().keySet().stream()).forEach(module -> {
            module.flag(ModuleCleanliness.DIRTY);
            dirty.add(module);
        });

        //#2 Compute clirty = all modules that depend on dirty or clirty modules
        clirty = new HashSet<>();

        //I need to flag all modules that depend on the dirty modules (recursively) as possibly dirty (clirty)
        //Using a DFS algorithm with the reverse dependency edges in the graph
        Set<IModule> visited = new HashSet<>(dirty);
        visited.addAll(removed);
        LinkedList<IModule> stack = new LinkedList<>(visited);
        while (!stack.isEmpty()) {
            IModule module = stack.pop();
            for (IModule depModule : module.getDependants().keySet()) {
                if (visited.contains(depModule)) continue;
                visited.add(depModule);
                depModule.flag(ModuleCleanliness.CLIRTY);
                clirty.add(depModule);
                stack.push(depModule);
            }
        }

        //#3 Compute clean = all - dirty - clean
        clean = all.stream().filter(m -> m.getFlag() == ModuleCleanliness.CLEAN).collect(Collectors.toSet());

        System.err.println("Based on the files, we identified:");
        System.err.println("  Dirty:  " + dirty.size()  + " modules (" + removed.size() + " removed)");
        System.err.println("  Clirty: " + clirty.size() + " modules");
        System.err.println("  Clean:  " + clean.size()  + " modules");

        //What are the semantics of the reanalysis?
        //First we want to collect the set of modules that needs to be redone for sure.
        //  - All modules dependent on removed modules need to be redone
        //  - All changed modules need to replace their old instances

        //Determine what has changed?
        //The spec needs to be executed from the module boundary again, on the new AST.
        //The new SG needs to be swapped in.
        //Queries of dependent modules (reversed relation?) need to be rechecked.
        //So their old results also need to be stored
    }

    public void validate() {
        for (IModule module : all) {
            if (module.getFlag() == ModuleCleanliness.NEW) throw new IllegalStateException("Module " + module.getId() + " is flagged as new!");
        }
        
        //Check that the children of each clean module are marked as clean as well
        for (IModule module : clean) {
            //The root is allowed to be clean even though it's children are not.
            if (ModulePaths.pathSegments(module.getId(), 2).length == 1) {
                System.err.println("Validation check cannot be removed");
                continue;
            }
            
            validateChildren(module, module, ModuleCleanliness.CLEAN);
        }
        
        //Check that the children of each removed module are also marked as deleted
        for (IModule module : removed) {
            validateChildren(module, module, ModuleCleanliness.DELETED);
        }
        
        //Check that the tree structure has the correct links
        //all() - removed()
        for (IModule module : all) {
            if (removed.contains(module)) continue;
            checkTreeIntegrity(module);
        }
    }
    
    private void checkTreeIntegrity(IModule module) {
        for (IModule child : module.getChildren()) {
            if (child.getParent() != module) {
                throw new IllegalStateException("Module " + child.getId() + " does not have the correct parent set. Is " + child.getParent() + ", should be " + module);
            } else if (!all.contains(child)) {
                throw new IllegalStateException("Module " + child.getId() + " is not in the set of all modules!");
            }
            
            checkTreeIntegrity(child);
        }
    }
    
    private boolean validateParent(IModule module, IModule base, ModuleCleanliness cleanliness) {
        IModule parent = module.getParent();
        if (parent == null) return true;
        switch (parent.getFlag()) {
            case CLEAN:   return validateParent(parent, base, cleanliness);
            case DELETED: throw new IllegalStateException(parent.getId() + " has been deleted, but " + base.getId() + " is marked as " + cleanliness + "!");
            case DIRTY:   throw new IllegalStateException(parent.getId() + " is marked as dirty, but " + base.getId() + " is marked as " + cleanliness + "!");
            case CLIRTY:  throw new IllegalStateException(parent.getId() + " is marked as clirty, but " + base.getId() + " is marked as " + cleanliness + "!");
            case NEW:     throw new IllegalStateException(parent.getId() + " is marked as new, which should not be possible (tree structure outdated)!");
            default:      throw new IllegalStateException(parent.getId() + " has an unknown flag: " + parent.getFlag());
        }
    }
    
    private void validateChildren(IModule module, IModule base, ModuleCleanliness cleanliness) {
        for (IModule child : module.getChildren()) {
            if (child.getFlag() == ModuleCleanliness.NEW) {
                throw new IllegalStateException(child.getId() + " is marked as new, which should not be possible (tree structure outdated)!");
            } else if (cleanliness == ModuleCleanliness.DELETED && child.getFlag() != cleanliness) {
                throw new IllegalStateException("Module " + base.getId() + " has been deleted, but its child " + child.getId() + " is marked as " + child.getFlag() + "!");
            } else if (cleanliness == ModuleCleanliness.CLEAN && child.getFlag() != cleanliness) {
                throw new IllegalStateException("Module " + base.getId() + " is marked clean, but its child " + child.getId() + " is marked as " + child.getFlag() + "!");
            } else {
                validateChildren(child, base, cleanliness);
            }
        }
    }

    @Override
    public Set<IModule> all() {
        return all;
    }

    @Override
    public Set<IModule> removed() {
        return removed;
    }

    @Override
    public Set<IModule> changed() {
        return changed;
    }

    @Override
    public Set<IModule> unchanged() {
        return unchanged;
    }

    @Override
    public Set<IModule> dirty() {
        return dirty;
    }

    @Override
    public Set<IModule> clirty() {
        return clirty;
    }

    @Override
    public Set<IModule> clean() {
        return clean;
    }
    
    @Override
    public Set<String> added() {
        return added;
    }

}
