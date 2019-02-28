package mb.statix.taico.solver;

import static mb.nabl2.terms.build.TermBuild.B;

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
public class MState {
    private final ModuleManager manager;
    private final SolverCoordinator coordinator;
    private final IModule owner;
    private Spec spec;
    private IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph;
    
    private int varCounter;
    private Set<ITermVar> vars;
    
    private int scopeCounter;
    private Set<ITerm> scopes;
    
    private volatile IUnifier.Immutable unifier;
    
    private ModuleSolver solver;
    
    public MState(ModuleManager manager, SolverCoordinator coordinator, IModule owner, Spec spec) {
        this.manager = manager;
        this.coordinator = coordinator;
        this.owner = owner;
        this.spec = spec;
        this.scopeGraph = owner.getScopeGraph();
        this.vars = new HashSet<>();
        this.scopes = new HashSet<>();
        this.unifier = PersistentUnifier.Immutable.of();
        
        owner.setCurrentState(this);
    }
    
    private MState(MState orig) {
        this.manager = orig.manager;
        this.coordinator = orig.coordinator;
        this.owner = orig.owner;
        this.spec = orig.spec;
        this.scopeGraph = orig.scopeGraph;
        this.solver = orig.solver;
        this.unifier = orig.unifier;
        
        //TODO CONCURRENT the copy needs to lock the old state in order to copy correctly.
        this.scopeCounter = orig.scopeCounter;
        this.scopes = new HashSet<>(orig.scopes);
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
        int i = ++varCounter;
        String name = base.replaceAll("-", "_") + "-" + i;
        ITermVar var = B.newVar(owner.getId(), name);
        vars.add(var);
        return var;
    }

    public Set<ITermVar> vars() {
        return this.vars;
    }

    // --- scopes ---

    public synchronized ITerm freshScope(String base) {
        return scopeGraph.createScope(base);
    }

    public Set<ITerm> scopes() {
        return scopes;
    }

    // --- solution ---

    public IUnifier.Immutable unifier() {
        return unifier;
    }
    
    public void setUnifier(IUnifier.Immutable unifier) {
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
}
