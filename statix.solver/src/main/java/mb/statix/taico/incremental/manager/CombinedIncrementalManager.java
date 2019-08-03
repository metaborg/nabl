package mb.statix.taico.incremental.manager;

import static mb.statix.taico.solver.Context.context;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.diff.Diff;
import mb.statix.taico.scopegraph.diff.DiffResult;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.coordinator.ISolverCoordinator;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.util.TDebug;

public class CombinedIncrementalManager extends IncrementalManager {
    private static final long serialVersionUID = 1L;
    private Map<IModule, IConstraint> initialModules;
    private Set<String> allowedBecauseNew;

    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------

    @Override
    public void setPhase(Object phase) {
        if (!(phase instanceof CombinedPhase)) {
            throw new IllegalArgumentException("Can only switch the phase to a combined phase.");
        }
        super.setPhase(phase);
    }
    
    @Override
    public void startFirstPhase(Map<IModule, IConstraint> modules) {
        System.out.println("[CIM] Starting first phase...");
        this.initialModules = modules;
        setPhase(CombinedPhase.DirtyStructure);
        
        //First we need to determine if there are circular dependencies on the changed files. If there are none, then there are no problems with just redoing the dirty modules
        //completely and skipping their structure.
        
        //TODO IMPLEMENTATION IMPORTANT IMPORTANT \/
        
        //1. Redo structure of dirty and added modules
        //2. Compare the structure and determine affected names
        //2b. Resolve as many names as possible, to reduce the number of unsure modules. (Resolves strict name dependencies in most languages)
        //3. Determine if there are circular dependencies.
        //   a. Yes: Redo all in the cycle + unsure modules
        //   b. No: Redo the remaining parts of the modules so far + unsure modules
        //We want to refine this at some point to redo as little unsure modules as possible
        
        //TODO IMPLEMENTATION IMPORTANT IMPORTANT /\
        
        //Distinguish between
        //  UnsureBecause(Name)Removed (Module has to be checked because a name is removed that it depended on)
        //  UnsureBecauseNameChanged (Module has to be checked because a name is changed that it depends on)
        
        //TODO Add the structure modules of the dirty modules and 
        
        //TODO check the absence of circular dependencies excluding names?
        
        //TODO Determine from the changeset which modules to add to the solver
        
        //We add all modules that are dirty or new, after which the solving process begins
        System.err.println("[CIM] Adding dirty and new modules");
        for (Entry<IModule, IConstraint> entry : modules.entrySet()) {
            final IModule module = entry.getKey();
            final IConstraint init = entry.getValue();
            System.out.println(module + ": " + module.getTopCleanliness());
            switch (module.getTopCleanliness()) {
                case DIRTY:
                case NEW: //TODO Do added modules get this flag?
                    ModuleSolver parentSolver = context().getState(module.getParentId()).solver();
                    parentSolver.childSolver(module.getCurrentState(), init);
                    break;
                default:
                    break;
            }
        }
        System.out.println("[CIM] Phase First started");
    }

    /**
     * 
     * 
     * @return
     *      true if we should switch to the dirty structure phase, false if we are already done
     */
    public boolean switchToDirtyStructurePhase(Set<ModuleSolver> finished,
            Set<ModuleSolver> failed, Set<ModuleSolver> stuck, Map<IModule, MSolverResult> results) {
        setPhase(CombinedPhase.DirtyStructure);
        //TODO Update the changeset with the new modules
        
        //TODO Currently this is the first phase, but we might want to have one before this?
        
        return false;
    }

    /**
     * @return
     *      true if we should switch to the unsure structure phase, false if we are already done
     */
    public boolean switchToUnsureStructurePhase(Set<ModuleSolver> finished,
            Set<ModuleSolver> failed, Set<ModuleSolver> stuck, Map<IModule, MSolverResult> results) {
        setPhase(CombinedPhase.UnsureStructure);
        //We have to determine all the circular dependencies that are still relevant now.

        //When solving an unsure module (partially), we need to keep some of its results from the previous run to compare against.
        //The completeness is just a counter, so it will work just fine. The store should have no results left.

        //We do not have to treat failing modules any differently. They have dependencies just like any other module.
        //If a module was previously failing and is now finished again immediately, its result will still report failure, which is good.

        
        //PLAN 2:
        //So far, all the dirty and new modules have had their structure completed. Now we need to compare it to our own structure
        //We need to have some form of split modules for this.
        
        //Ways for splitting modules
        //1. Put context-free in the main module and the rest in a separate one that becomes a child.
        //   
        return false;
    }

    /**
     * @return
     *      true if we should switch to the final phase, false if we are already done
     */
    public boolean switchToFinalPhase(Set<ModuleSolver> finished,
            Set<ModuleSolver> failed, Set<ModuleSolver> stuck, Map<IModule, MSolverResult> results) {
        
        setPhase(CombinedPhase.Final);
        //1. Diff the structures of the previous phases
        //2. Based on the diff, update the changeset with the changes (resolve some uncertainty)
        //3. Check for circular dependencies. Do we have any?
        //  a. Yes: Redo all in the cycle + remaining constraints of dirty/unsure modules
        //  b. No: Redo remaining constraints of dirty/unsure modules
        
        //TODO CUrrently ignores additions
        
        //TODO Update the changeset with the changes to dirty and unsure that we can conclude based on the results so far
//        context().getChangeSet().update();
        
        
        //Get all modules that are remaining and redo them in full
        //For each solver of currently dirty/unsure modules, redo them

        //If there are no stuck modules, that means that the structure of every module could be determined completely.
        //However, that does not mean we do not redo anything. It means that we need to compare the diffs.

        final ISolverCoordinator coordinator = context().getCoordinator();
        final IModule root = coordinator.getRootModule();
        System.err.println("[CIM] Diff in final phase");
        DiffResult diff = Diff.diff(root.getId(), context(), context().getOldContext().get(), true);
        diff.print(System.err);
        
        System.err.println("[CIM] Effective diff in final phase");
        diff.toEffectiveDiff().print(System.err);

        //At this point we have redone the structure of dirty modules and of unclean modules



        //Schedule all stuck modules for the next phase.
        System.out.println("[CIM] Scheduling all stuck modules for the Final phase: " + stuck);
        for (ModuleSolver solver : stuck) {
            solver.getStore().activateFromModules(TDebug.DEV_OUT);
            coordinator.addSolver(solver);
        }

        //TODO Do not always return true, sometimes we want to return false (no work left to do)
        return true;

        //Determine if we actually want this phase to start.

        //Option 1:
        //Coordinator has solvers, those solvers are removed as they are done.
        //Whenever a new round is started, solvers are added by the incremental manager.
        //Optionally, the solvers of the previous round can be passed to the manager.

        //Advantages:
        //- Clean and clear design, no extra work is done to determine the status

        //Disadvantages:
        //- The incremental manager needs to be able to find the solvers that need to be restarted still.

        //Option 2:
        //Coordinator has solvers, but solvers are not removed.
        //Whenever a new round is started, solvers are kept and those that now have work to do will start working again.

        //Advantages:
        //- Less bookkeeping?

        //Disadvantages:
        //- Solvers that are already done need to be rechecked to find they are done.
        //- Much more difficult with the concurrent solver
    }

    @Override
    public boolean finishPhase(Set<ModuleSolver> finished, Set<ModuleSolver> failed,
            Set<ModuleSolver> stuck, Map<IModule, MSolverResult> results) {
        switch (this.<CombinedPhase>getPhase()) {
            case DirtyStructure:
                if (switchToUnsureStructurePhase(finished, failed, stuck, results)) {
                    setPhase(CombinedPhase.UnsureStructure);
                    return true;
                }
                //Fallthrough
            case UnsureStructure:
                if (switchToFinalPhase(finished, failed, stuck, results)) {
                    setPhase(CombinedPhase.Final);
                    return true;
                }
                //Fallthrough
            case Final:
                if (switchToDirtyStructurePhase(finished, failed, stuck, results)) {
                    setPhase(CombinedPhase.DirtyStructure);
                    return true;
                }
    
                return false;
            default:
                throw new UnsupportedOperationException("Unknown phase " + getPhase());
        }
    }

    // --------------------------------------------------------------------------------------------
    // Access restrictions
    // --------------------------------------------------------------------------------------------

    @Override
    public boolean isAllowedAccess(String requester, String moduleId) {
        if (isInitPhase() || isAllowedTemporarily(requester)) return true;
        if (requester.equals(moduleId)) return true;
        
        switch (this.<CombinedPhase>getPhase()) {
            case DirtyStructure:
            case UnsureStructure:
                return false;
            case Final:
                return super.isAllowedAccess(requester, moduleId);
            default:
                throw new UnsupportedOperationException("Unknown phase " + getPhase());
        }
    }
    
    @Override
    public boolean createSplitModuleRequest(String id) {
        //If the given id is of a module we are only redoing the structure for, then return false
        //Otherwise, we return true for any normal module that is being redone.
        
        //In the final phase, for any module we redo, we should also redo the split module.
        if (getPhase() == CombinedPhase.Final) return true;
        
        //In all other phases, we only want to do this for dirty and new modules (we need to redo them in full)
        IModule module = context().getModuleUnchecked(id);
        switch (module.getTopCleanliness()) {
            case DIRTY:
            case NEW:
                return true;
            default:
                return false;
        }
    }

    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------

    @Override
    public void initSolver(ModuleSolver solver) {
        //TODO Is this the correct approach?
        //On initialization, we want to add the init constraint of the module if it is not clean
        IModule module = solver.getOwner();
        if (module.getTopCleanliness().isCleanish()) return;

        IMState state = solver.getOwner().getCurrentState();
        IConstraint init = solver.getOwner().getInitialization();
        if (init == null) {
            System.err.println("[CIM] Module " + solver.getOwner() + " does not have an init constraint.");
            return;
        }
        
        System.err.println("[CIM] Adding the init constraint of module " + solver.getOwner() + " to the completeness, to make it incomplete (it is uncertain)");
        
        solver.getCompleteness().add(init, state.unifier());
    }

    @Override
    public void solverStart(ModuleSolver solver) {
        System.err.println("[CIM] Solver start triggerd for " + solver.getOwner() + " (separate=" + solver.isSeparateSolver() + ")");


    }

    @Override
    public void solverDone(ModuleSolver solver) {
        System.err.println("[CIM] Solver done triggered for " + solver.getOwner() + ". Switching module over to clean.");

        //If this solver is done, do we count that as the solving having succeeded?
        super.solverDone(solver);
    }

    public static enum CombinedPhase {
        /** In this phase we redo the structure of dirty modules. */
        DirtyStructure,

        /** In this phase we redo the structure of unsure modules. */
        UnsureStructure,

        /** The final phase where we just do normal solving. */
        Final

    }
}

