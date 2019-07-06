package mb.statix.taico.solver.state;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.coordinator.ASolverCoordinator;
import mb.statix.taico.unifier.DistributedUnifier;
import mb.statix.taico.util.IOwnable;

public interface IMState extends IOwnable, Serializable, IState {
    public IModule owner();
    
    @Override
    public default String resource() {
        return owner().getId();
    }
    
    @Override
    public default IModule getOwner() {
        return owner();
    }
    
    @Override
    public default Spec spec() {
        return SolverContext.context().getSpec();
    }
    
    /**
     * Convenience method.
     * 
     * @see SolverContext#getCoordinator()
     */
    public default ASolverCoordinator coordinator() {
        return SolverContext.context().getCoordinator();
    }
    
    public ModuleSolver solver();
    
    public void setSolver(ModuleSolver solver);

    // --- variables ---

    public ITermVar freshVar(String base);
    
    /**
     * Same as {@link #freshVar(String)}, but does not add to vars.
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
    
    // --- term properties ---
    
    public Map<Tuple2<TermIndex, ITerm>, ITerm> termProperties();

    // --- scopes ---

    public default Scope freshScope(String base) {
        return scopeGraph().createScope(base);
    }

    @Override
    public default Set<Scope> scopes() {
        return scopeGraph().getScopes();
    }

    // --- solution ---

    @Override
    public DistributedUnifier.Immutable unifier();
    
    public void setUnifier(DistributedUnifier.Immutable unifier);

    @Override
    public IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph();
    
    // --- other ---
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
}
