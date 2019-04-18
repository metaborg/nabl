package mb.statix.taico.solver;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableTerm;

/**
 * Implementation of mutable state.
 */
public class MState implements IMState {
    private final ModuleManager manager;
    private final ISolverCoordinator coordinator;
    private final IModule owner;
    private Spec spec;
    private IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph;
    
    private int varCounter;
    private Set<ITermVar> vars;
    
    private volatile IUnifier.Immutable unifier;
    
    private ModuleSolver solver;
    
    public MState(ModuleManager manager, ISolverCoordinator coordinator, IModule owner, Spec spec) {
        this.manager = manager;
        this.coordinator = coordinator;
        this.owner = owner;
        this.spec = spec;
        this.scopeGraph = owner.getScopeGraph();
        this.vars = new HashSet<>();
        this.unifier = PersistentUnifier.Immutable.of();
        
        owner.setCurrentState(this);
    }
    
    /**
     * Protected constructor for {@link DelegatingMState} and {@link #copy()}.
     * 
     * @param original
     *      the original state
     * @param vars
     *      the new variables
     * @param scopeGraph
     *      the new scopeGraph
     */
    protected MState(MState original, Set<ITermVar> vars, IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph) {
        this.manager = original.manager();
        this.coordinator = original.coordinator();
        this.owner = original.owner();
        this.spec = original.spec();
        this.scopeGraph = scopeGraph;
        this.vars = vars;
        this.varCounter = original.varCounter;
        this.unifier = original.unifier();
        this.solver = original.solver();
    }
    
    @Override
    public IModule owner() {
        return owner;
    }
    
    @Override
    public Spec spec() {
        return spec;
    }
    
    @Override
    public ModuleManager manager() {
        return manager;
    }
    
    @Override
    public ISolverCoordinator coordinator() {
        return coordinator;
    }
    
    @Override
    public ModuleSolver solver() {
        return solver;
    }
    
    @Override
    public void setSolver(ModuleSolver solver) {
        this.solver = solver;
    }

    // --- variables ---

    @Override
    public synchronized ITermVar freshVar(String base) {
        //TODO Reuse freshRigidVar code?
        int i = ++varCounter;
        String name = base.replaceAll("-", "_") + "-" + i;
        ITermVar var = B.newVar(owner.getId(), name);
        vars.add(var);
        return var;
    }
    
    @Override
    public ITermVar freshRigidVar(String base) {
        int i = ++varCounter;
        String name = base.replaceAll("-", "_") + "-" + i;
        ITermVar var = B.newVar(owner.getId(), name);
        // same as freshVar, but do not add to vars
        return var;
    }

    @Override
    public Set<ITermVar> vars() {
        return this.vars;
    }

    // --- scopes ---

    @Override
    public synchronized IOwnableTerm freshScope(String base) {
        return scopeGraph.createScope(base);
    }

    @Override
    public Set<? extends IOwnableTerm> scopes() {
        return scopeGraph.getScopes();
    }

    // --- solution ---

    @Override
    public IUnifier.Immutable unifier() {
        return unifier;
    }
    
    @Override
    public void setUnifier(IUnifier.Immutable unifier) {
        this.unifier = unifier;
    }

    @Override
    public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph() {
        return scopeGraph;
    }

    // --- other ---
    @Override
    public synchronized DelegatingMState delegate(Set<ITermVar> vars, boolean clearScopes) {
        return new DelegatingMState(this, vars, clearScopes);
    }
    
    @Override
    public MState delegate() {
        return delegate(vars(), false);
    }
    
    /**
     * Creates a copy of this mutable state. The copy uses the same mutable scope graph, but all
     * other aspects are cloned.
     * 
     * <p>Any modifications made directly on the clone will not end up in the original state.</p>
     * 
     * @deprecated 
     *      Prefer {@link #delegate()} or {@link #delegate(Set, boolean)} over a copy.
     * 
     * @return
     *      a copy of this mutable state
     */
    @Deprecated
    public synchronized MState copy() {
        return new MState(this, new HashSet<>(vars()), scopeGraph().deepCopy());
    }
    
    /**
     * Updates this state to the given state.
     * The given state must be a clone from this state, and this state must not have been modified
     * after the copy.
     * 
     * @param state
     *      the state to update to
     * 
     * @throws IllegalArgumentException
     *      If the given state is not a copy of this state.
     * @throws ConcurrentModificationException
     *      If this state was modified after the copy was made.
     */
    public synchronized void updateTo(MState state) {
        if (state == this) return;
        if (this.owner != state.owner) throw new IllegalArgumentException("Cannot update to an unrelated state");
        
        System.err.println("Updating state of " + this.owner + " to copy");

        if (!state.vars.containsAll(this.vars)) {
            throw new ConcurrentModificationException("The original state was modified after the copy was made but before the updates were applied! (vars)");
        } else if (this.varCounter > state.varCounter) {
            throw new ConcurrentModificationException("The original state was modified after the copy was made but before the updates were applied! (varCounter)");
        }
        
        this.scopeGraph.updateToCopy(state.scopeGraph, false);
        this.unifier = state.unifier;
        this.varCounter = state.varCounter;
        this.vars = state.vars;
    }
}
