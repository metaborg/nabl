package mb.statix.taico.solver.progress;

import java.util.Collection;

import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.coordinator.ASolverCoordinator;
import mb.statix.taico.solver.state.IMState;

public class ProgressTracker {
    public int constraintsActive;
    public int constraintsDelayed;
    public int constraintsFailActive;
    public int constraintsFailDelayed;
    
    public int modulesTotal;
    public int modulesComplete;
    public int modulesFailed;
    
    public int solversTotal;
    public int solversRunning;
    public int solversComplete;
    
    public ProgressTracker() {}
    
    public synchronized void update() {
        Collection<IModule> modules = SolverContext.context().getModuleManager()._getModules();
        
        modulesTotal = modules.size();
        modulesComplete = 0;
        modulesFailed = 0;
        constraintsActive = 0;
        constraintsDelayed = 0;
        constraintsFailActive = 0;
        constraintsFailDelayed = 0;
        for (IModule module : modules) {
            IMState state = module.getCurrentState();
            if (state == null) continue;
            
            ModuleSolver solver = state.solver();
            if (solver == null) continue;
            
            boolean fail = solver.hasFailed();
            if (fail) modulesFailed++;
            
            final int active = solver.getStore().activeSize();
            final int delayed = solver.getStore().delayedSize();
            if (active + delayed == 0) {
                modulesComplete++;
            } else if (!fail) {
                constraintsActive += active;
                constraintsDelayed += delayed;
            } else {
                constraintsFailActive += active;
                constraintsFailDelayed += delayed;
            }
        }
        
        ASolverCoordinator coordinator = SolverContext.context().getCoordinator();
        solversRunning = coordinator.getSolvers().size();
        solversComplete = coordinator.getResults().size();
        solversTotal = solversRunning + solversComplete;
    }
    
    @Override
    public String toString() {
        return "Modules C/F/T (%)        : " + modulesComplete + "/" + modulesFailed + "/" + modulesTotal + " (" + (int) (((modulesComplete + modulesFailed) * 100) / (double) modulesTotal) + "%)\n"
             + "Solvers R/C/T            : " + + solversRunning + "/" + solversComplete + "/" + solversTotal + "\n"
             + "Constraints A/D/T (A/D/T): " + constraintsActive + "/" + constraintsDelayed + "/" + (constraintsActive + constraintsDelayed)
             + " (" + constraintsFailActive + "/" + constraintsFailDelayed + "/" + (constraintsFailActive + constraintsFailDelayed) + ")";
    }
    
}
