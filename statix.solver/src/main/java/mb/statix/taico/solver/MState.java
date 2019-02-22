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
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.scopegraph.OwnableScope;

public class MState {
    private final SolverCoordinator coordinator;
    private final IModule owner;
    private Spec spec;
    private IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph;
    
    private int varCounter;
    private Set<ITermVar> vars = new HashSet<>();
    
    private int scopeCounter;
    private Set<ITerm> scopes = new HashSet<>();
    
    private IUnifier.Immutable unifier = PersistentUnifier.Immutable.of();
    
    private ModuleSolver solver;
    
    public MState(SolverCoordinator coordinator, IModule owner, Spec spec) {
        this.coordinator = coordinator;
        this.owner = owner;
        this.spec = spec;
        this.scopeGraph = owner.getScopeGraph();
    }
    
    public IModule owner() {
        return owner;
    }
    
    public Spec spec() {
        return spec;
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

    public ITermVar freshVar(String base) {
        int i = ++varCounter;
        String name = owner.getId().replaceAll("-", "_") + "_" + base.replaceAll("-", "_") + "-" + i;
        ITermVar var = B.newVar("", name);
        vars.add(var);
        return var;
    }

    public Set<ITermVar> vars() {
        return this.vars;
    }

    // --- scopes ---

    public ITerm freshScope(String base) {
        int i = ++scopeCounter;
        
        String name = base.replaceAll("-", "_") + "-" + i;
        ITerm scope = new OwnableScope(owner, name);
        scopes.add(scope);
        return scope;
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

}
