package mb.statix.taico.incremental.manager;

import static mb.statix.taico.solver.Context.context;
import static mb.statix.taico.util.TPrettyPrinter.printScope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.NameDependencies;
import mb.statix.taico.dependencies.details.QueryDependencyDetail;
import mb.statix.taico.module.IModule;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.NameAndRelation;
import mb.statix.taico.scopegraph.diff.Diff;
import mb.statix.taico.scopegraph.diff.DiffResult;
import mb.statix.taico.scopegraph.diff.IScopeGraphDiff;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.util.Modules;

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
        processedModules.addAll(modules);
        redoReasons.clear();
        
        System.err.println("[NIM] TODO: Checking for cyclic dependencies COULD happen here");
        System.err.println("[NIM] Starting phase " + phaseCounter + " with modules " + modules);
        
        context().getCoordinator().preventSolverStart();
        for (IModule module : modules) {
            IConstraint init = initConstraints.get(module);
            if (init == null) init = module.getInitialization();
            
            ModuleSolver parentSolver = context().getState(module.getParentId()).solver();
            ModuleSolver oldSolver = context().getState(module).solver();
            parentSolver.childSolver(module.getCurrentState(), init);
            if (oldSolver != null) oldSolver.cleanUpForReplacement();
        }
        context().getCoordinator().allowSolverStart();
        System.err.println("[NIM] Phase " + phaseCounter + " started");
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
        
        DiffResult diff = new DiffResult();
        for (IModule module : results.keySet()) {
            Diff.diff(diff, module.getId(), context(), oldContext, true, false); // Last boolean might need to change to true depending on split modules
        }
        
        System.out.println("Diff result of phase " + phaseCounter + ":");
        diff.print(System.out);
        
        DiffResult eDiff = diff.toEffectiveDiff();
        System.out.println("Effective diff result of phase " + phaseCounter + ":");
        eDiff.print(System.out);
        
        Set<String> toRedo = new HashSet<>();
        
        //Now we need to check if the dependants are affected
        
        //For each modified scope graph
        //  -> For each module m that depends on us
        //      -> Check if m depends on the changes made
        //  -> For each module m that depends on us with dependency d
        //      -> Check if d is affected by changes to edges and scopes
        for (Entry<String, IScopeGraphDiff<Scope, ITerm, ITerm>> entry : eDiff.getDiffs().entrySet()) {
            final String changedModule = entry.getKey();
            final IScopeGraphDiff<Scope, ITerm, ITerm> sgDiff = entry.getValue();
            
            //For each dependant module, do a lookup of the corresponding names
            for (String dependant : oldContext.getDependencies(changedModule).getModuleDependantIds()) {
                if (eDiff.getRemovedModules().containsKey(dependant)) {
                    System.err.println("Encountered REMOVED dependant " + dependant + ", skipping");
                    continue;
                }
                NameDependencies dependencies = oldContext.getDependencies(dependant);
                
                //Iterate over the changes (hopefully the smallest set)
                checkNameDependencies(changedModule, toRedo, dependencies, sgDiff.getAddedDataNames().inverse());
                checkNameDependencies(changedModule, toRedo, dependencies, sgDiff.getRemovedDataNames().inverse());
                checkNameDependencies(changedModule, toRedo, dependencies, sgDiff.getChangedDataNames().inverse());
            }
            
            for (Entry<String, Dependency> entry2 : oldContext.getDependencies(changedModule).getDependants().entries()) {
                final String dependant = entry2.getKey();
                if (eDiff.getRemovedModules().containsKey(dependant)) {
                    System.err.println("Encountered REMOVED dependant " + dependant + ", skipping");
                    continue;
                }
                final Dependency dependency = entry2.getValue();
                QueryDependencyDetail qdetail = dependency.getDetails(QueryDependencyDetail.class);
                checkEdgeAndScopeDependencies(dependant, toRedo, qdetail, sgDiff);
            }
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
        Set<IModule> modules = Modules.toModules(moduleIds);
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

    /**
     * Checks if 
     * @param changedModule
     *      the module that was changed
     * @param toRedo
     *      the set of modules to redo
     * @param dependencies
     *      the dependencies of the dependent module
     * @param changed
     *      the changed names
     */
    private void checkNameDependencies(String changedModule, Set<String> toRedo,
            NameDependencies dependencies, IRelation3<Name, ITerm, Scope> changed) {
        //For each changed name
        //  -> Check if m has an entry for this name
        for (Entry<Tuple2<Name, ITerm>, Scope> entry : changed._getForwardMap().entrySet()) {
            final Tuple2<Name, ITerm> tuple = entry.getKey();
            final Scope scope = entry.getValue();
            NameAndRelation nar = tuple.getKey().withRelation(tuple.getValue());
            for (Dependency dependency : dependencies.getNameDependencies(nar, scope)) {
                String dependingModule = dependency.getOwner();
                System.out.println(dependingModule + " depends on " + changedModule + ", and is affected by change of name " + nar + " in " + printScope(scope));
                toRedo.add(dependingModule);
            }
        }
    }
    
    /**
     * Checks added edges, removed edges and removed scopes.
     * 
     * @param dependant
     *      the module that depends on the changed module
     * @param toRedo
     *      the set of modules to redo
     * @param detail
     *      the dependency detail for queries
     * @param sgDiff
     *      the diff of the changed module
     */
    private void checkEdgeAndScopeDependencies(String dependant, Set<String> toRedo, QueryDependencyDetail detail, IScopeGraphDiff<Scope, ITerm, ITerm> sgDiff) {
        for (Tuple2<Scope, ITerm> added : sgDiff.getAddedEdges()._getForwardMap().keySet()) {
            if (detail.canBeAffectedByEdgeAddition(added.getKey(), added.getValue())) {
                toRedo.add(dependant);
            }
        }
        
        for (Tuple2<Scope, ITerm> added : sgDiff.getRemovedEdges()._getForwardMap().keySet()) {
            if (detail.isAffectedByEdgeRemoval(added.getKey(), added.getValue())) {
                toRedo.add(dependant);
            }
        }
        
        for (Scope scope : sgDiff.getRemovedScopes()) {
            if (detail.isAffectedByScopeRemoval(scope)) {
                if (toRedo.add(dependant)) {
                    System.out.println("Scope removal was relevant for " + dependant + " (not yet added)! Scope " + printScope(scope));
                } else {
                    System.out.println("Scope removal was irrelevant for " + dependant + " (already added)! Scope " + printScope(scope));
                }
            }
        }
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

