package mb.statix.taico.incremental.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.constraint.CUser;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.SolverContext;

public abstract class IncrementalStrategy {
    public abstract void clearDirtyModules(IChangeSet changeSet, ModuleManager manager);
    
    /**
     * Reanalyzes modules in an incremental fashion depending on the strategy.
     * 
     * <p>This method should be called only after the {@link #setupReanalysis} method has been called.
     * 
     * @param changeSet
     *      the change set
     * @param baseState
     *      the state to start from
     * @param moduleConstraints
     *      a map from module name to constraints to solve
     * @param debug
     *      the debug context
     * 
     * @return
     *      a map of results, based on the changeset
     * 
     * @throws InterruptedException
     *      If solving is interrupted.
     */
    public abstract Map<String, ISolverResult> reanalyze(IChangeSet changeSet, IMState baseState, Map<String, Set<IConstraint>> moduleConstraints, IDebugContext debug) throws InterruptedException;
    
    /**
     * Gets a child module of the given requester. The strategy is free to
     * delay the request or to answer the request with an old version of the module.
     * 
     * @param context
     *      the solver context
     * @param oldContext
     *      the previous context (can be null)
     * @param requester
     *      the module requesting the access
     * @param childId
     *      the id of the child
     * 
     * @return
     *      the child module, or null if no child module exists.
     * 
     * @throws Delay
     *      If the child access needs to be delayed.
     */
    public abstract IModule getChildModule(SolverContext context, SolverContext oldContext, IModule requester, String childId) throws Delay;
    
    /**
     * Method for handling the request to get a module from the context. The strategy is free to
     * delay the request or to answer the request with an old version of the module.
     * 
     * @param context
     *      the context on which the module is requested
     * @param oldContext
     *      the previous context (can be null)
     * @param requester
     *      the requester of the module
     * @param id
     *      the id of the module being requested
     * 
     * @return
     *      the module that was requested, or null if such a module does not exist
     * 
     * @throws Delay
     *      If the access is not allowed (yet) in the current context phase.
     */
    public abstract IModule getModule(SolverContext context, SolverContext oldContext, IModule requester, String id) throws Delay;
    
    //---------------------------------------------------------------------------------------------
    public abstract void initializePhase(SolverContext context);
    
    /**
     * Creates / reuses the modules from the given map from module name to constraints.
     * This method assumes that each module has 0 or 1 constraint. If there is no constraint, the
     * initialization constraint for the module will be retrieved from the previous context.
     * If there is one constraint, it will be used as initialization reason for the module.
     * <p>
     * A state is also created for each module.
     * 
     * @param context
     *      the context
     * @param moduleConstraints
     *      the map from module name to the initialization constraints
     * 
     * @return
     *      a map from module to initialization constraints
     * 
     * @see SolverContext#getPhase()
     */
    public abstract Map<IModule, Set<IConstraint>> createModulesForPhase(SolverContext context, Map<String, Set<IConstraint>> moduleConstraints);
    
    /**
     * Called at the end of a phase (all modules are either done, failed or stuck).
     * If this method returns true, a new solver phase is started.
     * 
     * @param context
     *      the context
     *      
     * @return
     *      true if a new solver phase is required, false otherwise
     */
    public abstract boolean endOfPhase(SolverContext context);
    
    /**
     * Creates a new file module from the given entry.
     * 
     * This method will use {@link Entry#setValue(Object)} to alter the entry with the correct
     * initialization constraint if necessary.
     * 
     * @param context
     *      the context
     * @param entry
     *      the 
     * @return
     */
    protected IModule createFileModule(SolverContext context, Entry<String, Set<IConstraint>> entry) {
        String childName = entry.getKey();
        if (entry.getValue().size() > 1) {
            throw new IllegalArgumentException("Module " + childName + " has more than one initialization constraint: " + entry.getValue());
        }

        //Retrieve the child
        IModule child;
        if (entry.getValue().isEmpty()) {
            //Scope substitution does not have to occur here, since the global scope remains constant.
            //If there is no constraint available, use the initialization constraint for the child
            //Find the root module and get its child
            child = context.getModuleManager().getModuleByName(childName, 1);
            if (context.getModulesOnLevel(0).size() != 1) throw new IllegalStateException("Expected 1 root level module, found: " + context.getModulesOnLevel(0));
            
            child = context.getModulesOnLevel(0).values().stream().findFirst()
                    .map(m -> context.getModuleUnchecked(ModulePaths.build(m.getId(), childName)))
                    .orElse(null);
            if (child != null) entry.setValue(Collections.singleton(child.getInitialization()));
        } else {
            IConstraint initConstraint = getInitConstraint(entry.getValue());
            //TODO Instead get this from the old value?
            List<IOwnableScope> scopes = getScopes(context.getModuleManager(), initConstraint);
            
            IModule rootOwner;
            child = rootOwner.createOrGetChild(childName, scopes, initConstraint);
        }

        if (child == null) throw new IllegalStateException("Child " + childName + " could not be found!");
        return child;

        new MState(context, child, context.getSpec());
        modules.put(child, entry.getValue());
    }
    
    protected IConstraint getInitConstraint(Set<IConstraint> constraints) {
        IConstraint initConstraint = null;
        for (IConstraint constraint : constraints) {
            initConstraint = constraint;
            if (constraint instanceof CUser) return constraint;
        }
        return initConstraint;
    }
    
    /**
     * Determines the scopes in the arguments of the given constraint.
     * If the given constraint is not a CUser constraint, this method returns an empty list.
     * 
     * @param manager
     *      the manager to get modules from
     * @param constraint
     *      the constraint
     * 
     * @return
     *      the list of scopes in the given constraint
     */
    protected List<IOwnableScope> getScopes(ModuleManager manager, IConstraint constraint) {
        if (!(constraint instanceof CUser)) return Collections.emptyList();
        CUser user = (CUser) constraint;
        
        List<IOwnableScope> scopes = new ArrayList<>();
        for (ITerm term : user.args()) {
            Scope scope = Scope.matcher().match(term).orElse(null);
            if (scope != null) scopes.add(OwnableScope.fromScope(manager, scope));
        }
        return scopes;
    }
    
    /**
     * @return
     *      a matcher for incremental strategies
     */
    public static IMatcher<IncrementalStrategy> matcher() {
        Function<String, IncrementalStrategy> f = s -> {
            switch (s) {
                case "default":
                case "baseline":
                    return new BaselineIncrementalStrategy();
                //TODO Add more strategies here
                default:
                    return null;
            }
        };
        Function1<ITerm, Optional<IncrementalStrategy>> empty = i -> Optional.empty();
        return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<IncrementalStrategy>>cases(empty, empty,
                string -> Optional.ofNullable(f.apply(string.getValue())), empty, empty, empty));
    }
}
