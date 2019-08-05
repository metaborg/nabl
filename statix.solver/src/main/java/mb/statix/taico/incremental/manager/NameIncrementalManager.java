package mb.statix.taico.incremental.manager;

import static mb.statix.taico.solver.Context.context;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.NameDependencies;
import mb.statix.taico.dependencies.details.QueryDependencyDetail;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.NameAndRelation;
import mb.statix.taico.scopegraph.diff.Diff;
import mb.statix.taico.scopegraph.diff.DiffResult;
import mb.statix.taico.scopegraph.diff.ScopeGraphDiff;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.util.Modules;
import mb.statix.taico.util.TPrettyPrinter;

public class NameIncrementalManager extends IncrementalManager {
    private static final long serialVersionUID = 1L;
    private Map<IModule, IConstraint> initialModules;
    /**
     * The idea of this map is to keep track of all the modules that are processed in each phase.
     * This could be used to fix/detect circular dependencies.
     */
    private ListMultimap<Integer, IModule> phases = MultimapBuilder.hashKeys().arrayListValues().build();
    private int phaseCounter = -1;
    /**
     * A set of modules that have been processed by this solving process so far. If we encounter
     * the same modules again, we need to add them to the set that we are redoing, because they
     * must have a mutual dependency.
     */
    private Set<String> processedModules = new HashSet<>();

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
    public void startFirstPhase(Map<IModule, IConstraint> modules) {
        this.initialModules = modules;
        startPhase(modules.keySet());
    }
    
    private void startPhase(Set<IModule> modules) {
        phaseCounter++;
        setPhase(phaseCounter);
        phases.putAll(phaseCounter, modules);
        
        System.err.println("[NIM] TODO: Checking for cyclic dependencies COULD happen here");
        System.err.println("[NIM] Starting phase " + phaseCounter + " with modules " + modules);
        
        context().getCoordinator().preventSolverStart();
        for (IModule module : modules) {
            IConstraint init = initialModules.get(module);
            if (init == null) init = module.getInitialization();
            
            switch (module.getTopCleanliness()) {
                case DIRTY:
                case NEW:
                    ModuleSolver parentSolver = context().getState(module.getParentId()).solver();
                    parentSolver.childSolver(module.getCurrentState(), init);
                    break;
                default:
                    break;
            }
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
        
        Context oldContext = context().getOldContext().get();
        
        DiffResult diff = new DiffResult();
        for (Entry<IModule, MSolverResult> entry : results.entrySet()) {
            final IModule module = entry.getKey();
            final MSolverResult result = entry.getValue();
            if (result.hasErrors()) {
                //Module failed
                System.out.println("Module " + module + " failed. It is likely that the diff will be vast.");
            } else if (result.hasDelays()) {
                //Module got stuck
                System.out.println("Module " + module + " got stuck. It is likely that the diff will be vast.");
            }
            
            Diff.diff(diff, module.getId(), context(), oldContext, true, false); // Last boolean might need to change to true depending on split modules
        }
        
        System.out.println("Diff result of phase " + phaseCounter + ":");
        diff.print(System.out);
        
        DiffResult eDiff = diff.toEffectiveDiff();
        System.out.println("Effective diff result of phase " + phaseCounter + ":");
        eDiff.print(System.out);
        
        final IChangeSet changeSet = context().getChangeSet();
        Set<String> toRedo = new HashSet<>();
        
        //Now we need to check if the dependants are affected
        
        //For each modified scope graph
        //  -> For each module m that depends on us
        //      -> Check if m depends on the changes made
        //  -> For each module m that depends on us with dependency d
        //      -> Check if d is affected by changes to edges and scopes
        for (Entry<String, ScopeGraphDiff> entry : eDiff.getDiffs().entrySet()) {
            final String changedModule = entry.getKey();
            final ScopeGraphDiff sgDiff = entry.getValue();
            
            //For each dependant module, do a lookup of the corresponding names
            for (String dependant : oldContext.getDependencies(changedModule).getModuleDependantIds()) {
                if (changeSet.removedIds().contains(dependant)) {
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
                if (changeSet.removedIds().contains(dependant)) {
                    System.err.println("Encountered REMOVED dependant " + dependant + ", skipping");
                    continue;
                }
                final Dependency dependency = entry2.getValue();
                QueryDependencyDetail qdetail = dependency.getDetails(QueryDependencyDetail.class);
                checkEdgeAndScopeDependencies(dependant, toRedo, qdetail, sgDiff);
            }
        }
        
        //TODO IMPORTANT check for cyclic
        System.err.println("[NIM] TODO: Checking for cyclic dependencies SHOULD happen here");
        
        return toRedo;
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
            for (Dependency dependency : dependencies.get(nar, scope)) {
                String dependingModule = dependency.getOwner();
                System.out.println(dependingModule + " depends on " + changedModule + ", and is affected by change of name " + nar + " in " + TPrettyPrinter.printScopeFancy(scope));
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
    private void checkEdgeAndScopeDependencies(String dependant, Set<String> toRedo, QueryDependencyDetail detail, ScopeGraphDiff sgDiff) {
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
                    System.out.println("Scope removal was relevant for " + dependant + " (not yet added)! Scope " + TPrettyPrinter.printScopeFancy(scope));
                } else {
                    System.out.println("Scope removal was irrelevant for " + dependant + " (already added)! Scope " + TPrettyPrinter.printScopeFancy(scope));
                }
            }
        }
    }
    
    @Override
    public boolean finishPhase(Set<ModuleSolver> finished, Set<ModuleSolver> failed,
            Set<ModuleSolver> stuck, Map<IModule, MSolverResult> results) {
        int phase = getPhase();
        System.err.println("[NIM] Finished phase " + phase);
        
        Set<String> toRedo = diff(finished, failed, stuck, results);
        if (toRedo.isEmpty()) {
            System.out.println("[NIM] No modules left to redo, solving done :)");
            return false;
        }
        startPhaseWithIds(toRedo);
        return true;
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
        System.err.println("[NIM] Solver init triggered for " + solver.getOwner() + ". (separate=" + solver.isSeparateSolver() + ")");
        super.initSolver(solver);
    }

    @Override
    public void solverStart(ModuleSolver solver) {
        System.err.println("[NIM] Solver start triggered for " + solver.getOwner() + " (separate=" + solver.isSeparateSolver() + ")");
        super.solverStart(solver);
    }

    @Override
    public void solverDone(ModuleSolver solver, MSolverResult result) {
        System.err.println("[NIM] Solver done triggered for " + solver.getOwner() + " (separate=" + solver.isSeparateSolver() + ")");
        if (solver.isSeparateSolver()) return;
        
        results.put(solver.getOwner(), result);
        super.solverDone(solver, result);
    }
}

