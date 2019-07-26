package mb.statix.taico.module.split;

import static mb.statix.taico.module.ModulePaths.PATH_SEPARATOR;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.solver.store.ModuleConstraintStore;
import mb.statix.taico.unifier.DistributedUnifier;

public class SplitModuleUtil {
    //Split modules are modelled with the | char
    public static final char SPLIT_SEPARATOR_CHAR = '|';
    public static final String SPLIT_SEPARATOR = String.valueOf(SPLIT_SEPARATOR_CHAR);
    
    /**
     * Creates a new split module by taking all the delayed constraints from the given module and
     * using them as the initialization of the split module.
     * 
     * When the solver is created
     * 
     * @param module
     *      the module to create a split for
     * @param createSolver
     *      if true, a solver is created at the opportune moment
     * 
     * @return
     *      the split module
     */
    public static IModule createSplitModule(IModule module, boolean createSolver) {
        if (isSplitModule(module.getId())) throw new IllegalArgumentException("Cannot create a split for module " + module.getId() + ": module is already a split module.");
        System.err.println("Creating split module for " + module.getId());
        
        //We need to determine the canExtend set
        IMState contextFreeState = module.getCurrentState();
        IMInternalScopeGraph<Scope, ITerm, ITerm> contextFreeScopeGraph = contextFreeState.scopeGraph();
        Set<Scope> canExtendSet = contextFreeScopeGraph.getScopes();
        List<Scope> canExtendList = canExtendSet.stream().sorted((a, b) -> a.getName().compareTo(b.getName())).collect(Collectors.toList());
        canExtendList.addAll(contextFreeScopeGraph.getParentScopes());
        
        //Create the split module
        IModule split = module.createChild(SPLIT_SEPARATOR, canExtendList, null);
        IMState splitState = split.getCurrentState();
        
        //Clear and retrieve the delayed constraints on the original module.
        Set<IConstraint> delayed = contextFreeState.solver().getStore().delayedConstraints();
        
        //Create new variables for all variables of the original module that are delayed upon
        DistributedUnifier.Immutable cfUnifier = contextFreeState.unifier();
        
        //TODO Check if the unifier will not prevent top level variables from being used, since they could be composed of variables of the parent. - DONE
        //The above cannot happen, since the values passed to the module are ground and fully instantiated. In other words, they cannot contain any variables.
        
        //This can also be done with a unifier, but then we would also need to handle rigid vars and send the update.
//        Predicate1<ITermVar> isRigid = v -> !contextFreeState.vars().contains(v);
//        DistributedUnifier.Transient soFar = cfUnifier.melt();
        ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        Set<IConstraint> equalityConstraints = new HashSet<>();
        for (IConstraint constraint : delayed) {
            if (!(constraint instanceof CEqual)) continue;
            CEqual ce = (CEqual) constraint;
            
            //We need to determine the variables of the own module on which it is not ground (from the current module)
            Set<ITermVar> vars1 = cfUnifier.getOwnVariables(ce.term1());
            Set<ITermVar> vars2 = cfUnifier.getOwnVariables(ce.term2());
            for (ITermVar var : Sets.union(vars1, vars2)) {
                ITermVar nVar = splitState.freshVar(var.getName());
                subst.put(var, nVar);
                CEqual ce2 = new CEqual(var, nVar, ce);
                equalityConstraints.add(ce2);
//                try {
//                    soFar.unify(var, nVar, isRigid);
//                } catch (OccursException | RigidVarsException e) {
//                    System.err.println("Unable to create unification variable for split module " + split.getId() + ": " + var + " <-> " + nVar);
//                    e.printStackTrace();
//                }
            }
        }
        
        //We fix the initialization of the module to be the conjoined constraints with the given substitution applied
        IConstraint init = subst.isEmpty() ? Constraints.conjoin(delayed) : Constraints.conjoin(delayed).apply(subst.freeze());
        split.setInitialization(init);
        module.addChild(split);
        
        //Create the split solver if requested. Should happen before notifying the original of the new work.
        if (createSolver) {
            createSplitSolver(split);
        }
        
        //We need to notify the original module that there are more constraints
        ModuleConstraintStore store = contextFreeState.solver().getStore();
        store.addAll(equalityConstraints);
        store.notifyObserver();
        
        //To create the solver: contextFreeState.solver().childSolver(split.getCurrentState(), split.getInitialization());
        return split;
    }
    
    public static void updateSplitModule(IModule module) {
        //TODO update the split module with new constraints
        
    }
    
    /**
     * Creates the solver for a split module.
     * 
     * This method ensures that delayed constraints on the original are removed.
     * 
     * @param module
     *      the split module to create a solver for
     */
    public static void createSplitSolver(IModule module) {
        if (!isSplitModule(module.getId())) throw new IllegalArgumentException("Expected a split module, but was " + module.getId());
        System.err.println("Creating split solver for " + module.getId());
        
        SolverContext.context().getIncrementalManager().unregisterNonSplit(getMainModuleId(module.getId()));
        IMState parentState = SolverContext.context().getState(module.getParentId());
        parentState.solver().childSolver(module.getCurrentState(), module.getInitialization());
        
        Set<IConstraint> constraints = parentState.solver().getStore().clearDelays();
        parentState.solver().getCompleteness().removeAll(constraints, parentState.unifier());
    }
    
    /**
     * Determines the main module id of the given id (the context-free module).
     * The input id can be either a main module id or a split module id.
     * 
     * @param id
     *      the id of the module
     * 
     * @return
     *      the id of the main module
     */
    public static String getMainModuleId(String id) {
        if (!isSplitModule(id)) return id;
        return id.substring(0, id.length() - 2);
    }
    
    /**
     * @param id
     *      the id of the module
     * 
     * @return
     *      the id of the split module
     */
    public static String getSplitModuleId(String id) {
        if (isSplitModule(id)) return id;
        return id + PATH_SEPARATOR + SPLIT_SEPARATOR;
    }
    
    public static boolean isSplitModule(String id) {
        return id.endsWith(PATH_SEPARATOR + SPLIT_SEPARATOR);
    }
}
