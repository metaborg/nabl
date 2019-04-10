package mb.statix.taico.incremental.strategy;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.MState;

public interface IncrementalStrategy {
    /**
     * Reanalyzes using this incremental strategy.
     * 
     * @param manager
     *      the module manager to set the modules to be solved in (TODO)
     * @param changeSet
     *      the change set
     */
    void setupReanalysis(ModuleManager manager, IChangeSet changeSet);

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
