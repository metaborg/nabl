package mb.statix.taico.incremental.strategy;

import java.util.Map;
import java.util.Set;

import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.SolverContext;

public class NonIncrementalStrategy extends IncrementalStrategy {
    @Override
    public void clearDirtyModules(IChangeSet changeSet, ModuleManager manager) {
        manager.clearModules();
    }
    
    @Override
    public Map<String, ISolverResult> reanalyze(IChangeSet changeSet, IMState baseState, Map<String, Set<IConstraint>> constraints, IDebugContext debug)
            throws InterruptedException {
        //TODO Ensure everything is redone, but that we remember old modules.
        return baseState.coordinator().solve(this, changeSet, baseState, constraints, debug);
    }
    
    @Override
    public IModule getModule(SolverContext context, SolverContext oldContext, IModule requester, String id)
            throws Delay {
        return context.getModuleManager().getModule(id);
    }
    
    @Override
    public IModule getChildModule(SolverContext context, SolverContext oldContext, IModule requester, String childId) throws Delay {
        //Always allow child access when we are not being incremental
        return context.getModuleManager().getModule(childId);
    }
}
