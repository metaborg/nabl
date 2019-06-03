package mb.statix.taico.incremental.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.SolverContext;

public class NonIncrementalStrategy extends IncrementalStrategy {
    @Override
    public Map<String, ISolverResult> reanalyze(IChangeSet changeSet, IMState baseState, Map<String, IConstraint> constraints, IDebugContext debug)
            throws InterruptedException {
        //TODO Ensure everything is redone, but that we remember old modules.
        return baseState.coordinator().solve(this, changeSet, baseState, constraints, debug);
    }
    
    @Override
    public IModule getModule(SolverContext context, SolverContext oldContext, IModule requester, String id) {
        return context.getModuleManager().getModule(id);
    }
    
    @Override
    public IModule getChildModule(SolverContext context, SolverContext oldContext, IModule requester, String childId) {
        //Always allow child access when we are not being incremental
        return context.getModuleManager().getModule(childId);
    }
    
    @Override
    public Map<IModule, IConstraint> createModulesForPhase(SolverContext context,
            IChangeSet changeSet,
            Map<String, IConstraint> moduleConstraints) {
        Map<IModule, IConstraint> newModules = new HashMap<>();
        for (Entry<String, IConstraint> entry : moduleConstraints.entrySet()) {
            IModule module = createFileModule(context, entry.getKey(), entry.getValue());
            newModules.put(module, entry.getValue());
        }
        
        return newModules;
    }
    
    @Override
    public boolean endOfPhase(SolverContext context) {
        return false;
    }
}
