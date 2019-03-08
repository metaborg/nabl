package mb.statix.taico.solver;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.util.Capsules;

/**
 * Implementation of mutable state.
 */
public class MState {
    private final ModuleManager manager;
    private final SolverCoordinator coordinator;
    private final IModule owner;
    private Spec spec;
    private IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph;
    
    private int varCounter;
    private Set<ITermVar> vars;
    
    private volatile IUnifier.Immutable unifier;
    
    private ModuleSolver solver;
    
    public MState(ModuleManager manager, SolverCoordinator coordinator, IModule owner, Spec spec) {
        this.manager = manager;
        this.coordinator = coordinator;
        this.owner = owner;
        this.spec = spec;
        this.scopeGraph = owner.getScopeGraph();
        this.vars = new HashSet<>();
        this.unifier = PersistentUnifier.Immutable.of();
        
        owner.setCurrentState(this);
    }
    
    private MState(MState orig) {
        this.manager = orig.manager;
        this.coordinator = orig.coordinator;
        this.owner = orig.owner;
        this.spec = orig.spec;
        //TODO The parent should also be copied, we need a tree copy of the entire scope graph for consistency.
        this.scopeGraph = orig.scopeGraph.deepCopy(orig.scopeGraph.getParent());
        this.solver = orig.solver;
        this.unifier = orig.unifier;
        this.varCounter = orig.varCounter;
        this.vars = new HashSet<>(orig.vars);
    }
    
    private MState(MState orig, boolean shallow) {
        this.manager = orig.manager;
        this.coordinator = orig.coordinator;
        this.owner = orig.owner;
        this.spec = orig.spec;
        this.scopeGraph = orig.scopeGraph;
        this.solver = orig.solver;
        this.unifier = orig.unifier;
        this.varCounter = orig.varCounter;
        this.vars = new HashSet<>(orig.vars);
    }
    
    public IModule owner() {
        return owner;
    }
    
    public Spec spec() {
        return spec;
    }
    
    public ModuleManager manager() {
        return manager;
    }
    
    public SolverCoordinator coordinator() {
        return coordinator;
    }
    
    public ModuleSolver solver() {
        return solver;
    }
    
    public void setSolver(ModuleSolver solver) {
        this.solver = solver;
    }

    // --- variables ---

    public synchronized ITermVar freshVar(String base) {
        System.err.println("          Adding new variable " + base + "-" + (varCounter + 1) + " to " + this);
        int i = ++varCounter;
        String name = base.replaceAll("-", "_") + "-" + i;
        ITermVar var = B.newVar(owner.getId(), name);
        vars.add(var);
        return var;
    }

    public Set<ITermVar> vars() {
        if (!vars.toString().equals(Capsules.newSet(vars).toString())) {
            System.out.println("vars: " + vars + " copy: " + new HashSet<>(vars));
        } else {
            System.out.println("vars: " + vars + " copy: equal");
        }
        return new HashSet<>(this.vars);
//        return Collections.unmodifiableSet(this.vars);
    }

    // --- scopes ---

    public synchronized IOwnableTerm freshScope(String base) {
        return scopeGraph.createScope(base);
    }

    public Set<? extends IOwnableTerm> scopes() {
        return scopeGraph().getScopes();
    }

    // --- solution ---

    public IUnifier.Immutable unifier() {
        return unifier;
    }
    
    public void setUnifier(IUnifier.Immutable unifier) {
        System.err.println("[" + owner.getId() + "] Setting unifier on " + this);
        this.unifier = unifier;
    }

    public IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph() {
        return scopeGraph;
    }

    // --- other ---
    /**
     * Creates a copy of this mutable state. The copy uses the same mutable scope graph, but all
     * other aspects are cloned.
     * 
     * <p>Any modifications made directly on the clone will not end up in the original state.</p>
     * 
     * @return
     *      a copy of this mutable state
     */
    public synchronized MState copy() {
        return new MState(this);
    }
    
    public synchronized MState shallowCopy() {
        return new MState(this, true);
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

        System.err.println("Updating state of @" + owner.getId());
        varDiff(state);
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
    
    public void varDiff(MState copy) {
        System.err.println("new vars in original: " + Sets.difference(this.vars, copy.vars));
        System.err.println("new vars in copy:     " + Sets.difference(copy.vars, this.vars));
    }
}
