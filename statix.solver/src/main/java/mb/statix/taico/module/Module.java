package mb.statix.taico.module;

import static mb.statix.taico.solver.SolverContext.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.ModuleScopeGraph;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.query.QueryDetails;

/**
 * Basic implementation of {@link IModule}. The identifiers are not automatically generated.
 */
//TODO This would be a StatixModule or SGModule
public class Module implements IModule {
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private String parentId;
    private volatile String cachedId;
    private IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph;
    private Map<CResolveQuery, QueryDetails> queries = new HashMap<>();
    private Map<String, CResolveQuery> dependants = new ConcurrentHashMap<>();
    protected ModuleCleanliness cleanliness = ModuleCleanliness.NEW;
    private IConstraint initialization;
    
    private PriorityQueue<Flag> flags = new PriorityQueue<>();
    
    /**
     * Creates a new top level module.
     * 
     * @param name
     *      the name of the module
     * @param spec
     *      the spec
     */
    public Module(String name) {
        this(name, true);
    }
    
    /**
     * Protected constructor for {@link DelegatingModule}.
     * 
     * @param name
     *      the name of the module
     * @param addToContext
     *      if true, adds to the context, otherwise, doesn't alter the context
     */
    protected Module(String name, boolean addToContext) {
        Spec spec = context().getSpec();
        
        this.name = name;
        this.scopeGraph = new ModuleScopeGraph(this, spec.edgeLabels(), spec.relationLabels(), spec.noRelationLabel(), Collections.emptyList());
        if (addToContext) context().addModule(this);
    }
    
    /**
     * Constructor for creating child modules.
     * 
     * @param name
     *      the name of the child
     * @param parent
     *      the parent module
     */
    private Module(String name, IModule parent) {
        this.name = name;
        this.parentId = parent == null ? null : parent.getId();
        context().addModule(this);
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getId() {
        final String id = cachedId;
        if (id != null) return id;
        return cachedId = (parentId == null ? name : ModulePaths.build(parentId, name));
    }
    
    @Override
    public IModule getParent() {
        System.err.println("Getting parent on module " + this);
        return parentId == null ? null : context().getModuleUnchecked(parentId);
    }
    
    @Override
    public void setParent(IModule module) {
        //TODO Because of how the parent system currently works, we cannot hang modules under different parents.
        //     This is because we currently do not transitively update the module ids in our children as well.
        this.parentId = module == null ? null : module.getId();
        this.cachedId = null;
    }

    @Override
    public IMInternalScopeGraph<Scope, ITerm, ITerm> getScopeGraph() {
        return scopeGraph;
    }

    @Override
    public Module createChild(String name, List<Scope> canExtend, IConstraint constraint) {
        Module child = new Module(name, this);
        child.setInitialization(constraint);
        child.scopeGraph = scopeGraph.createChild(child, canExtend);
        return child;
    }
    
    @Override
    public IConstraint getInitialization() {
        return initialization;
    }
    
    @Override
    public void setInitialization(IConstraint constraint) {
        this.initialization = constraint;
    }
    
    @Override
    public Set<IModule> getDependencies() {
        return queries.values().stream().flatMap(d -> d.getReachedModules().stream()).map(d -> context().getModuleUnchecked(d)).collect(Collectors.toSet());
    }
    
    @Override
    public void addQuery(CResolveQuery query, QueryDetails details) {
        queries.put(query, details);
    }
    
    @Override
    public Map<CResolveQuery, QueryDetails> queries() {
        return queries;
    }
    
    @Override
    public void addDependant(String module, CResolveQuery query) {
        dependants.put(module, query);
    }
    
    @Override
    public Map<IModule, CResolveQuery> getDependants() {
        return dependants.entrySet().stream()
                .collect(Collectors.toMap(e -> context().getModuleUnchecked(e.getKey()), Entry::getValue));
    }
    
    @Override
    public PriorityQueue<Flag> getFlags() {
        return flags;
    }
    
    @Override
    public void flag(ModuleCleanliness cleanliness) {
        this.cleanliness = cleanliness;
    }
    
    @Override
    public ModuleCleanliness getFlag() {
        return cleanliness;
    }
    
    @Override
    public void reset(Spec spec) {
        this.scopeGraph = new ModuleScopeGraph(this, scopeGraph.getEdgeLabels(), scopeGraph.getDataLabels(), scopeGraph.getNoDataLabel(), scopeGraph.getParentScopes());
        this.queries = new HashMap<>();
        this.dependants = new HashMap<>();
        this.cleanliness = ModuleCleanliness.NEW;
        new MState(this);
        context().addModule(this);
    }
    
    // --------------------------------------------------------------------------------------------
    // Copy
    // --------------------------------------------------------------------------------------------

    /**
     * Creates a copy of this module and it's scope graph.
     * 
     * Please note that the created copy is not added to the context.
     * 
     * @return
     *      the copy
     */
    @Override
    public Module copy() {
        return new Module(this);
    }

    /**
     * Copy constructor. The module is not added to the context.
     * 
     * @param original
     *      the module to copy
     */
    private Module(Module original) {
        this.name = original.name;
        this.parentId = original.parentId;
        this.cleanliness = original.cleanliness;
        this.queries = new HashMap<>(original.queries);
        this.dependants = new ConcurrentHashMap<>(original.dependants);
        this.initialization = original.initialization;
        this.scopeGraph = original.scopeGraph.copy(this);
    }

    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Module)) return false;
        assert !this.getId().equals(((Module) obj).getId()) : "Module identifiers are equal but modules are not the same instance! (id: " + getId() + ")";
        return this.getId().equals(((Module) obj).getId());
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() + (parentId == null ? 0 : (31 * parentId.hashCode()));
    }
    
    @Override
    public String toString() {
        return "@" + getId();
    }
}
