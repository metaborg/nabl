package mb.statix.taico.solver;

import org.metaborg.util.log.Level;

import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.PrefixedDebugContext;

public class EntailsCoordinator extends SolverCoordinator {
    @Override
    public void addSolver(ModuleSolver solver) {
        //TODO This should maybe be possible
        throw new IllegalStateException("Cannot add additional solvers to entails coordinator!");
    }
    
    public MSolverResult entails(MState state, Iterable<IConstraint> constraints, IDebugContext debug)
            throws InterruptedException {
        //TODO IMPORTANT Implement this?
        rootState = state;
        root = ModuleSolver.topLevelSolver(state, constraints, debug);
        solvers.add(root);
        
        PrefixedDebugContext cdebug = new PrefixedDebugContext("Coordinator", debug);
        
        boolean anyProgress = true;
        ModuleSolver progressed = root; //The solver that has progressed, or null if multiple
        outer: while (anyProgress && !solvers.isEmpty()) {
            cdebug.log(Level.Trace, "Begin of main loop");
            anyProgress = false;
            
            //Ensure that changes are not reflected this run
            
            ModuleSolver[] tempSolvers = solvers.toArray(new ModuleSolver[0]);
            for (ModuleSolver solver : tempSolvers) {
                cdebug.log(Level.Trace, "Checking solver for module {}", solver.getOwner().getId());
                //If this solver is done, store its result and continue.
                if (solver.isDone()) {
                    cdebug.log(Level.Debug, "[{}] done, removing...", solver.getOwner().getId());
                    solvers.remove(solver);
                    results.put(solver.getOwner(), solver.finishSolver());
                    continue;
                }
                cdebug.log(Level.Trace, "[{}] is not done", solver.getOwner().getId());
                
                //TODO Improve the mechanism of delaying and "someone progresses, so just redo all"
                //If this solver is not the only solver that has made progress last round, then inform it that someone else has made progress.
                if (solver != progressed) {
                    cdebug.log(Level.Debug, "[{}] informed of external progress by {}", solver.getOwner().getId(), progressed);
                    solver.externalProgress();
                } else {
                    cdebug.log(Level.Debug, "[{}] is the only solver who made progress last round", solver.getOwner().getId());
                }
                
                cdebug.log(Level.Debug, "[{}] going to try solve a step", solver.getOwner().getId());
                //If any progress can be made, store that information
                if (solver.solveStep()) {
                    cdebug.log(Level.Debug, "[{}] solved one step", solver.getOwner().getId());
                    if (anyProgress) {
                        progressed = null;
                    } else {
                        anyProgress = true;
                        progressed = solver;
                    }
                    
                    //Solve this solver as far as possible
                    while (solver.solveStep());
                } else {
                    cdebug.log(Level.Debug, "[{}] unable to solve a step", solver.getOwner().getId());
                }
                
                
                if (solver.hasFailed()) {
                    cdebug.log(Level.Debug, "[{}] failed", solver.getOwner().getId());
                    break outer;
                }
            }
            
            cdebug.log(Level.Trace, "end of main loop");
        }
        
        LazyDebugContext lazyDebug = new LazyDebugContext(cdebug);
        //If we end up here, none of the solvers is still able to make progress
        if (solvers.isEmpty()) {
            //All solvers are done!
            lazyDebug.info("All solvers finished successfully!");
        } else {
            lazyDebug.warn("Solving failed, {} unusuccessful solvers: ", solvers.size());
            IDebugContext sub = lazyDebug.subContext();
            for (ModuleSolver solver : solvers) {
                sub.warn(solver.getOwner().getId());
                
                results.put(solver.getOwner(), solver.finishSolver());
            }
            
            solvers.clear();
        }
        debug(lazyDebug);
        lazyDebug.commit();
        
        return aggregateResults();
    }
}
