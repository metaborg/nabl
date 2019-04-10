package mb.statix.taico.incremental.strategy;

import java.util.HashSet;
import java.util.Set;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.MState;

/**
 * Incremental strategy which is naive and simply redoes all modules that have changed and all
 * modules that depend on them (transitively) (dirty U clirty).
 * 
 * In other words, only modules that are unchanged and that do not transitively depend on modules
 * which are changed, are redone.
 */
public class BaselineIncrementalStrategy implements IncrementalStrategy {
    @Override
    public void setupReanalysis(ModuleManager manager, IChangeSet changeSet) {
        //TODO: redo dirty and clirty, with clean in the context
        Set<IModule> redo = new HashSet<>(changeSet.dirty());
        redo.addAll(changeSet.clirty());

        //Nothing to do, there were no changes
        if (redo.isEmpty()) return;

        //TODO IMPORTANT We need to exclude the top level module from the purge
        //TODO TESTING Verify that the top level module does not cause a purge of all modules

        //Delete all modules that need to be redone as well as their children
        for (IModule module : redo) manager.purgeModules(module);
    }

    /**
     * Reanalyzes the modules that are not marked as clean.
     * 
     * @param baseState
     *      the state to start from
     * @param constraints
     *      the constraints to solve
     * @param debug
     *      the debug context
     * 
     * @throws InterruptedException
     *      If solving is interrupted.
     */
    public void reanalyze(MState baseState, Iterable<IConstraint> constraints, IDebugContext debug) throws InterruptedException {
        //Solve from the top again, children will be skipped automatically.
        baseState.coordinator().solve(baseState, constraints, debug);
    }
}
