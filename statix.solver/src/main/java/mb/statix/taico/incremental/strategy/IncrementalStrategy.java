package mb.statix.taico.incremental.strategy;

import java.util.Set;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.MState;

public interface IncrementalStrategy {
    /**
     * Reanalyzes using this incremental strategy.
     * 
     * @param manager
     *      the module manager to set the modules to be solved in (TODO)
     * @param unchanged
     *      the set of modules that are unchanged
     * @param dirty
     *      the set of modules that are dirty
     * @param clirty
     *      the set of modules that are potentially dirty
     * @param clean
     *      the set of modules that are definitely clean
     */
    void setupReanalysis(ModuleManager manager, Set<IModule> unchanged, Set<IModule> dirty, Set<IModule> clirty, Set<IModule> clean);
    
    /**
     * Reanalyzes modules in an incremental fashion depending on the strategy.
     * 
     * <p>This method should be called only after the {@link #setupReanalysis} method has been called.
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
    void reanalyze(MState baseState, Iterable<IConstraint> constraints, IDebugContext debug) throws InterruptedException;
}
