package mb.statix.taico.solver;

import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.util.IOwnable;

public interface IMState extends IOwnable {
    public IModule owner();
    
    @Override
    public default IModule getOwner() {
        return owner();
    }
    
    public Spec spec();
    
    public ModuleManager manager();
    
    public ASolverCoordinator coordinator();
    
    public void setCoordinator(ASolverCoordinator coordinator);
    
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

    public Set<ITermVar> vars();

    // --- scopes ---

    public default IOwnableTerm freshScope(String base) {
        synchronized (this) {
            return scopeGraph().createScope(base);
        }
    }

    public default Set<? extends IOwnableTerm> scopes() {
        return scopeGraph().getScopes();
    }

    // --- solution ---

    public IUnifier.Immutable unifier();
    
    public void setUnifier(IUnifier.Immutable unifier);

    public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph();
    
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
