package mb.statix.taico.incremental;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.module.ModuleManager;

public class ChangeSet implements IChangeSet {
    private Set<IModule> all;
    private Set<IModule> removed, changed, unchanged;
    private Set<IModule> dirty, clirty, clean;

    public ChangeSet(ModuleManager manager, Set<String> removed, Set<String> changed) {
        this.removed = removed.stream().map(manager::getModule).collect(Collectors.toSet());
        this.changed = changed.stream().map(manager::getModule).collect(Collectors.toSet());

        init(manager);
    }

    private void init(ModuleManager manager) {
        //Mark the initial status of all modules
        all = new HashSet<>(manager.getModules());
        all.forEach(m -> m.flag(ModuleCleanliness.CLEAN));
        removed.forEach(m -> m.flag(ModuleCleanliness.DELETED));

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

}
