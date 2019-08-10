package mb.statix.modular.solver.state;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.util.Tuple2;
import mb.statix.modular.module.IModule;
import mb.statix.modular.scopegraph.IMInternalScopeGraph;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.coordinator.ISolverCoordinator;
import mb.statix.modular.unifier.DistributedUnifier;
import mb.statix.modular.util.IOwnable;
import mb.statix.modular.util.ScopeIdentity;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.spec.Spec;

/**
 * Interface to represent mutable state.
 */
public interface IMState extends IOwnable, Serializable, IState {
    // --------------------------------------------------------------------------------------------
    // Identity
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      the owner of this state
     */
    public IModule owner();
    
    @Override
    public default String resource() {
        return owner().getId();
    }
    
    @Override
    public default IModule getOwner() {
        return owner();
    }
    
    // --------------------------------------------------------------------------------------------
    // Convenience
    // --------------------------------------------------------------------------------------------
    
    @Override
    public default Spec spec() {
        return Context.context().getSpec();
    }
    
    // --------------------------------------------------------------------------------------------
    // Solver
    // --------------------------------------------------------------------------------------------
    
    /**
     * Convenience method. Returns the current coordinator based on the context.
     * 
     * @see Context#getCoordinator()
     */
    public default ISolverCoordinator coordinator() {
        return Context.context().getCoordinator();
    }
    
    /**
     * Returns the solver that is currently responsible for progressing this state.
     * 
     * @return
     *      the solver
     */
    public ModuleSolver solver();
    
    public void setSolver(@Nullable ModuleSolver solver);

    // --------------------------------------------------------------------------------------------
    // Variables
    // --------------------------------------------------------------------------------------------

    /**
     * Creates a new variable in this module, with the given base name.
     * 
     * @param base
     *      the base name for the variable
     * 
     * @return
     *      the created variable
     */
    public ITermVar freshVar(String base);
    
    /**
     * Same as {@link #freshVar(String)}, but does not add to the vars in this state.
     * 
     * @param base
     *      the base name for the variable
     * 
     * @return
     *      the created variable
     */
    public ITermVar freshRigidVar(String base);

    @Override
    public Set<ITermVar> vars();
    
    // --------------------------------------------------------------------------------------------
    // Term properties
    // --------------------------------------------------------------------------------------------
    
    @Override
    public Map<Tuple2<TermIndex, ITerm>, ITerm> termProperties();

    // --------------------------------------------------------------------------------------------
    // Scopes
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a new scope with the given base name. The created scope will be unique as long as
     * base is unique within the given constraint. 
     * 
     * @param base
     *      the base name
     * @param constraint
     *      the constraint which causes the creation of this scope
     * 
     * @return
     *      a newly created scope
     */
    public default Scope freshScope(String base, @Nullable IConstraint constraint) {
        StringBuilder sb = new StringBuilder();
        sb.append(base);
        ScopeIdentity.userTrace(constraint, sb);
        return scopeGraph().createScopeWithIdentity(sb.toString());
    }

    @Override
    public default Set<Scope> scopes() {
        return scopeGraph().getScopes();
    }

    // --------------------------------------------------------------------------------------------
    // Solution
    // --------------------------------------------------------------------------------------------

    @Override
    public DistributedUnifier.Immutable unifier();
    
    public void setUnifier(DistributedUnifier.Immutable unifier);

    @Override
    public IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph();
    
    // --------------------------------------------------------------------------------------------
    // Views / copies
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a delegate of this state with the given set of variables kept and scopes cleared
     * (optionally). A delegate is a view on the original state.
     * Any modifications are local to the delegate.
     * Changes to the variables or scopes of the original state are reflected in the delegate.
     * 
     * @param vars
     *      the variables to keep, will be intersected with the current variables
     * @param clearScopes
     *      if true, the delegate will have no scopes
     * 
     * @return
     *      the delegating state
     */
    public IMState delegate(Set<ITermVar> vars, boolean clearScopes);
    
    /**
     * Creates a delegate of this state. A delegate is a view on the original state.
     * Any modifications are local to the delegate.
     * Changes to the variables or scopes of the original state are reflected in the delegate.
     * 
     * @return
     *      a delegate with the same variables and no scopes cleared
     * 
     * @see #delegate(Set, boolean)
     */
    public default IMState delegate() {
        return delegate(vars(), false);
    }
    
    /**
     * Creates a copy of this state, replacing the scope graph with the given graph.
     * 
     * @param graph
     *      the scope graph for the copy
     * 
     * @return
     *      the copy
     */
    public IMState copy(IMInternalScopeGraph<Scope, ITerm, ITerm> graph);
    
    /**
     * Creates a copy of this state with a copy of the scope graph.
     * 
     * @return
     *      the copy     
     */
    public default IMState copy() {
        return copy(scopeGraph().copy());
    }
}
