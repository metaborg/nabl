package mb.statix.taico.module.split;

import static mb.statix.taico.module.ModulePaths.PATH_SEPARATOR;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.state.IMState;

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
     * 
     * @return
     *      the split module
     */
    public static IModule createSplitModule(IModule module) {
        if (isSplitModule(module.getId())) throw new IllegalArgumentException("Cannot create a split for module " + module.getId() + ": module is already a split module.");
        System.err.println("Creating split module for " + module.getId());
        
        //We need to determine the canExtend set
        IMState contextFreeState = module.getCurrentState();
        IMInternalScopeGraph<Scope, ITerm, ITerm> contextFreeScopeGraph = contextFreeState.scopeGraph();
        Set<Scope> canExtendSet = contextFreeScopeGraph.getScopes();
        List<Scope> canExtendList = canExtendSet.stream().sorted((a, b) -> a.getName().compareTo(b.getName())).collect(Collectors.toList());
        canExtendList.addAll(contextFreeScopeGraph.getParentScopes());
        
        //Clear and retrieve the delayed constraints on the original module.
        Set<IConstraint> delayed = contextFreeState.solver().getStore().delayedConstraints();
        
        //Just store the original initialization and associate special behavior to the split module?
        IConstraint init = Constraints.conjoin(delayed);
        
        IModule split = module.createChild(SPLIT_SEPARATOR, canExtendList, init);
        module.addChild(split);
        
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
