package mb.statix.modular.incremental.manager;

import static mb.statix.modular.solver.Context.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.unification.IUnifier;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.Modules;
import mb.statix.modular.scopegraph.diff.Diff;
import mb.statix.modular.scopegraph.diff.DiffResult;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.unifier.DistributedUnifier;
import mb.statix.modular.util.TDebug;
import mb.statix.solver.IConstraint;

public class NameIncrementalManager extends IncrementalManager {
    private static final long serialVersionUID = 1L;
    private Map<IModule, IConstraint> initConstraints;
    /**
     * The idea of this map is to keep track of all the modules that are processed in each phase.
     * This could be used to fix/detect circular dependencies.
     */
    private SetMultimap<Integer, IModule> phases = MultimapBuilder.hashKeys().hashSetValues().build();
    private SetMultimap<Integer, IModule> actualPhases = MultimapBuilder.hashKeys().hashSetValues().build();
    /**
     * Stores the reason why the module is in the set of modules to redo in the last phase.
     * Cleared at the start of each phase.
     */
    private Map<String, RedoReason> redoReasons = new HashMap<>();
    private int phaseCounter = -1;
    /**
     * A set of modules that have been processed by this solving process so far. If we encounter
     * the same modules again, we need to add them to the set that we are redoing, because they
     * must have a mutual dependency.
     */
    private Set<IModule> processedModules = new HashSet<>();

    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------

    @Override
    public void setPhase(Object phase) {
        if (!(phase instanceof Integer)) {
            throw new IllegalArgumentException("Can only switch the phase to a numbered phase.");
        }
        super.setPhase(phase);
    }
    
    @Override
    public void startFirstPhase(Map<IModule, IConstraint> initConstraints) {
        this.initConstraints = initConstraints;
        Set<IModule> modules = initConstraints.keySet().stream()
                .filter(this::acceptFirstPhaseModule)
                .collect(Collectors.toSet());
        startPhase(modules);
    }
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      true if this module should be scheduled for the first phase, false otherwise
     */
    private boolean acceptFirstPhaseModule(IModule module) {
        switch (module.getTopCleanliness()) {
            case DIRTY:
            case NEW:
                return true;
            default:
                return false;
        }
    }
    
    public void startPhase(Set<IModule> modules) {
        phaseCounter++;
        setPhase(phaseCounter);
        phases.putAll(phaseCounter, modules);
        
        System.err.println("[NIM] TODO: Checking for cyclic dependencies COULD happen here");
        
        processedModules.addAll(modules);
        redoReasons.clear();
        
        System.err.println("[NIM] Starting phase " + phaseCounter + " with modules " + modules);
        
        Context context = context();
        context.getCoordinator().preventSolverStart();
        for (IModule module : modules) {
            IConstraint init = initConstraints.get(module);
            if (init == null) {
                init = module.getInitialization();
            } else {
                module.setInitialization(init);
            }
            
            ModuleSolver parentSolver = context.getSolver(module.getParentId());
            ModuleSolver oldSolver = context.getSolver(module);
            
            //Determine descendants BEFORE resetting the scope graph (which clears children)
            Set<IModule> descendants = module.getDescendants().collect(Collectors.toSet());
            
            //Reset the unifier, completeness and IMPORTANT the scope graph 
            resetModule(module, true);
            ModuleSolver newSolver = parentSolver.childSolver(module.getCurrentState(), init, oldSolver);
            if (oldSolver != null) oldSolver.replaceWith(newSolver);
            
            //Reset children
            for (IModule child : descendants) {
                resetModule(child, true);
            }
            //TODO IMPORTANT #12 We need to remove the child modules, (remove their state / reset their state). Otherwise we can get stale values from the children, breaking the solving process!.
        }
        context().getCoordinator().allowSolverStart();
        System.err.println("[NIM] Phase " + phaseCounter + " started");
    }

    /**
     * Resets the module.
     * <p>
     * Normally, the only way to obtain a variable from a child module is when that child module
     * is already created. However, when redoing modules, it would still be possible to retrieve
     * old values from the child module from another module. To prevent this, we have to reset the
     * module to some kind of fresh state.
     * <p>
     * This method does 5 things:               <br>
     * 1. Completeness = delay all local        <br>
     * 2. Completeness -= own delays + failures <br>
     * 3. Unifier = {}                          <br>
     * 4. ScopeGraph = {}                       <br>
     * 5. Dependencies = {}
     * 
     * @param module
     *      the module to reset
     * @param clearScopeGraph
     *      if true, the scope graph will be cleared
     */
    private void resetModule(IModule module, boolean clearScopeGraph) {
        IMState state = module.getCurrentState();
        if (state == null) return;
        
        System.err.println("Resetting module " + module);
        ModuleSolver solver = state.solver();
        if (solver == null) return;
        
        //1. Completeness = delay all local
        solver.getCompleteness().switchDelayMode(true);
        
        //2. Completeness -= own delays + fails
        //(this can activate observers, which will immediately be delayed again)
        IUnifier oldUnifier = state.unifier();
        solver.getCompleteness().removeAll(solver.getStore().delayedConstraints(), oldUnifier);
        solver.getCompleteness().removeAll(solver.getFailed(), oldUnifier);
        
        //3. Clear unifier
        state.setUnifier(DistributedUnifier.Immutable.of(module.getId()));
        
        //4. Clear scope graph
        if (clearScopeGraph) state.scopeGraph().clear();
        
        context().resetDependencies(module.getId());
    }

    /**
     * Starts a phase with the given modules.
     * 
     * @param moduleIds
     *      the ids of the modules to start
     */
    public void startPhaseWithIds(Set<String> moduleIds) {
        Set<IModule> modules = Modules.toModulesRemoveNull(moduleIds);
        startPhase(modules);
    }

    /**
     * @param finished
     *      all the solvers that finished in the phase that just finished
     * @param failed
     *      all the solvers that failed in the phase that just finished
     * @param stuck
     *      all the solvers that got stuck in the phase that just finished
     * @param results
     *      the results of solving
     * 
     * @return
     *      the modules to redo
     */
    public Set<String> diff(Set<ModuleSolver> finished, Set<ModuleSolver> failed,
            Set<ModuleSolver> stuck, Map<IModule, MSolverResult> results) {
        System.err.println("[NIM] Computing diff to determine modules for next phase");
        
        Context oldContext = context().getOldContext();
        if (oldContext == null) throw new IllegalStateException("The old context should not be null!");
        
        DiffResult eDiff = new DiffResult();
        for (IModule module : results.keySet()) {
            Diff.effectiveDiff(eDiff, new HashSet<>(), module.getId(), context(), oldContext, true, false); // Last boolean might need to change to true depending on split modules
        }
        
        System.out.println("Effective diff result of phase " + phaseCounter + ":");
        eDiff.print(System.out);
        
        //TODO IMPORTANT Assert that there are no more dependencies on modules that have been removed, since those modules will be redone.
        //Determine the dependencies
        Set<String> toRedo = eDiff.getDependencies(context(), Dependency::getOwner);
        
        //Do not redo any removed modules
        for (String module : eDiff.getRemovedModules()) {
            toRedo.remove(module);
        }
        
        for (String id : toRedo) {
            redoReasons.put(id, RedoReason.DIFF);
        }
        
        //TODO IMPORTANT check for cyclic
        System.err.println("[NIM] TODO: Checking for cyclic dependencies SHOULD happen here");
        
        return toRedo;
    }
    
    /**
     * Normalizes the modules to redo by doing the following:
     * 
     * 1) removing all child modules of modules also in the set<br>
     * 2) removing all modules that were also redone in the last phase because of the diff
     * 
     * 
     * @param moduleIds
     *      the ids of the modules
     * 
     * @return
     *      the set of modules to redo
     */
    private Set<IModule> normalizeToRedo(Set<String> moduleIds) {
        Set<IModule> modules = Modules.toModulesRemoveNull(moduleIds); //TODO IMPORTANT Temporary removeNull
        Set<IModule> doneInLastPhase = actualPhases.get(getPhase());
        Iterator<IModule> it = modules.iterator();
        while (it.hasNext()) {
            IModule module = it.next();
            
            //If we redid this module in the last phase, then we should not redo it again in the next phase
            //TODO Unless if things are circular and we want to redo it again
            if (doneInLastPhase.contains(module) && redoReasons.get(module.getId()) == RedoReason.DIFF) {
                it.remove();
                continue;
            }
            
            for (IModule parent : module.getParents()) {
                if (modules.contains(parent)) {
                    it.remove();
                    break;
                }
            }
        }
        
        return modules;
    }
    
    @Override
    public boolean finishPhase(Set<ModuleSolver> finished, Set<ModuleSolver> failed,
            Set<ModuleSolver> stuck, Map<IModule, MSolverResult> results) {
        final Set<IModule> actualModules = determineActualModules(finished, failed, stuck);
        
        int phase = getPhase();
        //TODO Check if we get results for the stub solvers that we created for the unchanged solvers. We want to ignore these.
        System.err.println("[NIM] Finished phase " + phase + " with modules " + actualModules);
        
        actualPhases.putAll(phase, actualModules);
        processedModules.addAll(actualModules);
        
        //Do not compute a diff if this was a clean run
        if (actualModules.containsAll(context().getModulesOnLevel(1).values())) {
            System.out.println("[NIM] Last phase was the equivalent of a clean run, solving done :/");
            return false;
        }
        
        Set<String> toRedoIds = diff(finished, failed, stuck, results);
        Set<IModule> toRedo = normalizeToRedo(toRedoIds);
        if (toRedo.isEmpty()) {
            System.out.println("[NIM] No modules left to redo, solving done :)");
            return false;
        }
        
        startPhase(toRedo);
        return true;
    }

    private Set<IModule> determineActualModules(Set<ModuleSolver> finished, Set<ModuleSolver> failed, Set<ModuleSolver> stuck) {
        Set<IModule> actualModules = new HashSet<>();
        for (ModuleSolver solver : finished) {
            if (solver.isNoopSolver()) continue;
            actualModules.add(solver.getOwner());
        }
        for (ModuleSolver solver : failed) {
            if (solver.isNoopSolver()) continue;
            actualModules.add(solver.getOwner());
        }
        for (ModuleSolver solver : stuck) {
            if (solver.isNoopSolver()) continue;
            actualModules.add(solver.getOwner());
        }
        return actualModules;
    }

    // --------------------------------------------------------------------------------------------
    // Access restrictions
    // --------------------------------------------------------------------------------------------

//    @Override
//    public boolean isAllowedAccess(String requester, String moduleId) {
//        if (isInitPhase() || isAllowedTemporarily(requester)) return true;
//        if (requester.equals(moduleId)) return true;
//        
//        switch (this.<NamePhase>getPhase()) {
//            case DirtyStructure:
//            case UnsureStructure:
//                return false;
//            case Final:
//                return super.isAllowedAccess(requester, moduleId);
//            default:
//                throw new UnsupportedOperationException("Unknown phase " + getPhase());
//        }
//    }
//    
//    @Override
//    public boolean createSplitModuleRequest(String id) {
//        //If the given id is of a module we are only redoing the structure for, then return false
//        //Otherwise, we return true for any normal module that is being redone.
//        
//        //In the final phase, for any module we redo, we should also redo the split module.
//        if (getPhase() == NamePhase.Final) return true;
//        
//        //In all other phases, we only want to do this for dirty and new modules (we need to redo them in full)
//        IModule module = context().getModuleUnchecked(id);
//        switch (module.getTopCleanliness()) {
//            case DIRTY:
//            case NEW:
//                return true;
//            default:
//                return false;
//        }
//    }

    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------

    @Override
    public void initSolver(ModuleSolver solver) {
        if (solver.isSeparateSolver()) return;
        System.err.println("[NIM] Solver init triggered for " + solver.getOwner());
        super.initSolver(solver);
    }

    @Override
    public void solverStart(ModuleSolver solver) {
        if (solver.isSeparateSolver()) return;
        System.err.println("[NIM] Solver start triggered for " + solver.getOwner());
        super.solverStart(solver);
    }

    @Override
    public void solverDone(ModuleSolver solver, MSolverResult result) {
        if (solver.isSeparateSolver()) return;
        System.err.println("[NIM] Solver done triggered for " + solver.getOwner());
        
        results.put(solver.getOwner(), result);
        super.solverDone(solver, result);
    }
    
    // --------------------------------------------------------------------------------------------
    // Redo reason
    // --------------------------------------------------------------------------------------------
    
    public static enum RedoReason {
        DIFF,
        CIRCULAR
    }
}

