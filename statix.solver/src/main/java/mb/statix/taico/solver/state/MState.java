package mb.statix.taico.solver.state;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.ModuleScopeGraph;
import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.unifier.DistributedUnifier;
import mb.statix.taico.util.ScopeIdentity;

/**
 * Implementation of mutable state.
 */
public class MState implements IMState {
    private static final long serialVersionUID = 1L;
    
    private final IModule owner;
    private final IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph;
    private final Map<Tuple2<TermIndex, ITerm>, ITerm> termProperties;
    
    private volatile int varCounter;
    private final Set<ITermVar> vars;
    
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
    public MState(IModule owner, IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph) {
        this.owner = owner;
        this.scopeGraph = scopeGraph;
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
    protected MState(MState original, Set<ITermVar> vars, IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph) {
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

    // --------------------------------------------------------------------------------------------
    // Variables
    // --------------------------------------------------------------------------------------------

    @Override
    public ITermVar freshVar(String base) {
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
    
    // --------------------------------------------------------------------------------------------
    // Term properties
    // --------------------------------------------------------------------------------------------
    
    @Override
    public Map<Tuple2<TermIndex, ITerm>, ITerm> termProperties() {
        return termProperties;
    }

    // --------------------------------------------------------------------------------------------
    // Scopes
    // --------------------------------------------------------------------------------------------

    @Override
    public Scope freshScope(String base, IConstraint constraint) {
        StringBuilder sb = new StringBuilder();
        sb.append(base);
        ScopeIdentity.userTrace(constraint, sb);
        return scopeGraph.createScopeWithIdentity(sb.toString());
    }

    @Override
    public Set<Scope> scopes() {
        return scopeGraph.getScopes();
    }

    // --------------------------------------------------------------------------------------------
    // Solution
    // --------------------------------------------------------------------------------------------

    @Override
    public DistributedUnifier.Immutable unifier() {
        return unifier;
    }
    
    @Override
    public void setUnifier(DistributedUnifier.Immutable unifier) {
        this.unifier = unifier;
    }

    @Override
    public IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph() {
        return scopeGraph;
    }

    // --------------------------------------------------------------------------------------------
    // Views / copies
    // --------------------------------------------------------------------------------------------
    
    @Override
    public synchronized DelegatingMState delegate(Set<ITermVar> vars, boolean clearScopes) {
        return new DelegatingMState(this, vars, clearScopes);
    }
    
    @Override
    public MState delegate() {
        return delegate(vars(), false);
    }
    
    @Override
    public MState copy(IMInternalScopeGraph<Scope, ITerm, ITerm> graph) {
        return new MState(this, new HashSet<>(vars), graph);
    }
    
    // --------------------------------------------------------------------------------------------
    // Static creation
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a new top level state for the given module.
     * <p>
     * NOTE: Uses the context to retrieve the state.
     * 
     * @param module
     *      the module
     * 
     * @return
     *      the state for the given module
     */
    public static MState topLevelState(IModule module) {
        Spec spec = SolverContext.context().getSpec();
        ModuleScopeGraph scopeGraph = new ModuleScopeGraph(module, spec.edgeLabels(), spec.relationLabels(), spec.noRelationLabel(), Collections.emptyList());
        return new MState(module, scopeGraph);
    }
}
