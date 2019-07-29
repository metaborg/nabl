package mb.statix.taico.module;

import static mb.statix.taico.solver.SolverContext.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.solver.query.QueryDetails;
import mb.statix.taico.solver.state.MState;
import mb.statix.taico.util.StablePriorityQueue;
import mb.statix.taico.util.TOverrides;

/**
 * Basic implementation of {@link IModule}. The identifiers are not automatically generated.
 */
public class Module implements IModule {
    private static final long serialVersionUID = 1L;
    
    private final String name;                                             //Stateless
    private final String parentId;                                         //Stateless
    private final String id;                                               //Stateless
    private Map<CResolveQuery, QueryDetails> queries = new HashMap<>();    //Stateful, old value unimportant (?)
    private Map<String, CResolveQuery> dependants = TOverrides.hashMap();  //Stateful, old value unimportant (?)
    private IConstraint initialization;                                    //Stateful, old value unimportant
    private StablePriorityQueue<Flag> flags = new StablePriorityQueue<>(); //Stateful, old value unimportant
    
    /**
     * Creates a new top level module.
     * 
     * @param name
     *      the name of the module
     * @param spec
     *      the spec
     */
    private Module(String name) {
        this.name = name;
        this.parentId = null;
        this.id = (parentId == null ? name : ModulePaths.build(parentId, name));
        context().addModule(this);
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
        this.id = (parentId == null ? name : ModulePaths.build(parentId, name));
        context().addModule(this);
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public Module createChild(String name, List<Scope> canExtend, IConstraint constraint) {
        System.err.println("Creating child module " + name + " on " + this);
        Module child = new Module(name, this);
        child.setInitialization(constraint);
        new MState(child, getScopeGraph().createChild(child, canExtend));
        return child;
    }
    
    @Override
    public StablePriorityQueue<Flag> getFlags() {
        return flags;
    }
    
    // --------------------------------------------------------------------------------------------
    // Initialization
    // --------------------------------------------------------------------------------------------
    
    @Override
    public IConstraint getInitialization() {
        return initialization;
    }
    
    @Override
    public void setInitialization(IConstraint constraint) {
        this.initialization = constraint;
    }
    
    // --------------------------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------------------------
    
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
    public Map<String, CResolveQuery> getDependantIds() {
        return dependants;
    }
    
    @Override
    public void resetDependants() {
        this.dependants = TOverrides.hashMap();
        this.queries = new HashMap<>();
    }
    
    // --------------------------------------------------------------------------------------------
    // Copy
    // --------------------------------------------------------------------------------------------

    /**
     * Creates a copy of this module, but not it's state.
     * 
     * Please note that the created copy is not added to the context.
     * 
     * @return
     *      the copy
     * 
     * @deprecated
     *      Since modules have no <b>important</b> stateful fields, there is no need to ever copy
     *      one. Instead, a copy should be made of the state.
     */
    @Override
    @Deprecated
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
        this.id = original.id;
        this.flags = new StablePriorityQueue<>(original.flags);
        this.queries = new HashMap<>(original.queries);
        this.dependants = TOverrides.hashMap(original.dependants);
        this.initialization = original.initialization;
    }

    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Module)) return false;
        
        assert !id.equals(((Module) obj).getId()) : "Module identifiers are equal but modules are not the same instance! (id: " + id + ")";
        return id.equals(((Module) obj).getId());
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return id;
    }
    
    // --------------------------------------------------------------------------------------------
    // Static initialization
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a new top level module and its corresponding state.
     * 
     * @param name
     *      the name of the module
     * 
     * @return
     *      the created module
     */
    public static Module topLevelModule(String name) {
        Module module = new Module(name);
        MState.topLevelState(module);
        return module;
    }
}
