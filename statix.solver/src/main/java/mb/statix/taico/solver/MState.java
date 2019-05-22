package mb.statix.taico.solver;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.unifier.DistributedUnifier;

/**
 * Implementation of mutable state.
 */
public class MState implements IMState, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final IModule owner;
    private IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> scopeGraph;
    private Map<Tuple2<TermIndex, ITerm>, ITerm> termProperties;
    
    private int varCounter;
    private Set<ITermVar> vars;
    
    private volatile DistributedUnifier.Immutable unifier;
    
    private transient ModuleSolver solver;
    
    /**
     * Constructor for creating a new state for the given module.
     * <p>
     * <b>NOTE</b>: this constructor sets the state of the module to itself.
     * 
     * @param owner
     *      the owner of this state
     */
    public MState(IModule owner) {
        this.owner = owner;
        this.scopeGraph = owner.getScopeGraph();
        this.vars = new HashSet<>();
        this.unifier = DistributedUnifier.Immutable.of(owner.getId());
        this.termProperties = new HashMap<>();
        SolverContext.context().setState(owner, this);
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
    protected MState(MState original, Set<ITermVar> vars, IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> scopeGraph) {
        this.owner = original.owner();
        this.scopeGraph = scopeGraph;
        this.vars = vars;
        this.varCounter = original.varCounter;
        this.unifier = original.unifier();
        this.solver = original.solver();
        this.termProperties = new HashMap<>();
    }
    
    @Override
    public IModule owner() {
        return owner;
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
    
    @Override
    public Map<Tuple2<TermIndex, ITerm>, ITerm> termProperties() {
        return termProperties;
    }

    // --- scopes ---

    @Override
    public AScope freshScope(String base) {
        return scopeGraph.createScope(base);
    }

    @Override
    public Set<? extends AScope> scopes() {
        return scopeGraph.getScopes();
    }

    // --- solution ---

    @Override
    public DistributedUnifier.Immutable unifier() {
        return unifier;
    }
    
    @Override
    public void setUnifier(DistributedUnifier.Immutable unifier) {
        this.unifier = unifier;
    }

    @Override
    public IMInternalScopeGraph<AScope, ITerm, ITerm, ITerm> scopeGraph() {
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
}
