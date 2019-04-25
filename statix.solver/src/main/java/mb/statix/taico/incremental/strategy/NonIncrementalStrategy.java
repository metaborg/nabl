package mb.statix.taico.incremental.strategy;

import java.util.Map;
import java.util.Set;

import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.IMState;

public class NonIncrementalStrategy implements IncrementalStrategy {

    @Override
    public void setupReanalysis(ModuleManager manager, IChangeSet changeSet) {
        manager.clearModules();
    }

    @Override
    public Map<String, ISolverResult> reanalyze(IChangeSet changeSet, IMState baseState, Map<String, Set<IConstraint>> constraints, IDebugContext debug)
            throws InterruptedException {
        return baseState.coordinator().solve(baseState, constraints, debug);
    }
}
