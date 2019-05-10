package mb.statix.taico.incremental.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.constraint.CUser;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.SolverContext;

public abstract class IncrementalStrategy {
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
    // Phasing
    //---------------------------------------------------------------------------------------------

    /**
     * Creates / reuses the modules from the given map from module name to constraints.
     * This method assumes that each module has 1 constraint which will be used as initialization
     * reason for the module.
     * <p>
     * All modules in the returned map will have a solver created for them, all modules that are
     * not in the map will be available, but won't be actively solving themselves.
     * <p>
     * Implementors should use {@link #createFileModule(SolverContext, String, Set)} and
     * {@link #reuseOldModule(SolverContext, IModule)} to create or reuse modules.
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
     * Creates a new file module from the given module and initconstraints.
     * Strategies can override this method to change the behavior.
     * 
     * @param context
     *      the context
     * @param entry
     *      the (modifiable) entry
     * 
     * @return
     *      the created module
     * @throws Delay 
     */
    protected IModule createFileModule(SolverContext context, String childName, Set<IConstraint> initConstraints) {
        System.err.println("[IS] Creating file module for " + childName);
        if (initConstraints.size() != 1) {
            throw new IllegalArgumentException("Module " + childName + " does not have exactly 1 initialization constraint: " + initConstraints);
        }

        IConstraint initConstraint = getInitConstraint(initConstraints);
        List<AScope> scopes = getScopes(initConstraint);
        
        IModule rootOwner = context.getRootModule();
        IModule child = rootOwner.createChild(childName, scopes, initConstraint);
        new MState(child);
        return child;
    }
    
    /**
     * Reuses an old module for a new analysis.
     * This method creates a state and a dummy solver for the given module.
     * 
     * @param context
     *      the context
     * @param oldModule
     *      the old module
     */
    protected void reuseOldModule(SolverContext context, IModule oldModule) {
        System.err.println("[IS] Reusing old module " + oldModule);
        MState state = new MState(oldModule);
        //TODO Is the root solver set at this point?
        context.getRootModule().getCurrentState().solver().noopSolver(state);
    }
    
    /**
     * Retrieves the single init constraint from the set of constraints.
     * @param constraints
     * @return
     */
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
     * @param constraint
     *      the constraint
     * 
     * @return
     *      the list of scopes in the given constraint
     */
    protected List<AScope> getScopes(IConstraint constraint) {
        if (!(constraint instanceof CUser)) return Collections.emptyList();
        CUser user = (CUser) constraint;
        
        List<AScope> scopes = new ArrayList<>();
        for (ITerm term : user.args()) {
            Scope scope = Scope.matcher().match(term).orElse(null);
            if (scope != null) scopes.add(scope);
        }
        return scopes;
    }
    
    //---------------------------------------------------------------------------------------------
    
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
