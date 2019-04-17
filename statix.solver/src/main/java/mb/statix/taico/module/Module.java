package mb.statix.taico.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.spec.Spec;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.scopegraph.ModuleScopeGraph;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.query.QueryDetails;

/**
 * Basic implementation of {@link IModule}. The identifiers are not automatically generated.
 */
//TODO This would be a StatixModule or SGModule
public class Module implements IModule {
    private final String name;
    private final ModuleManager manager;
    private IModule parent;
    private IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph;
    private IMState state;
    private Map<CResolveQuery, QueryDetails<IOwnableTerm, ITerm, ITerm>> queries = new HashMap<>();
    private Map<IModule, CResolveQuery> dependants = new HashMap<>();
    private ModuleCleanliness cleanliness = ModuleCleanliness.NEW;
    
    /**
     * Creates a new top level module.
     * 
     * @param name
     *      the name of the module
     * @param labels
     *      the labels on edges of the scope graph
     * @param endOfPath
     *      the label that indicates the end of a path
     * @param relations
     *      the labels on data edges of the scope graph
     */
    public Module(ModuleManager manager, String name, Iterable<ITerm> labels, ITerm endOfPath, Iterable<ITerm> relations) {
        this.manager = manager;
        this.name = name;
        this.parent = null;
        this.scopeGraph = new ModuleScopeGraph(this, labels, endOfPath, relations, Collections.emptyList());
        manager.addModule(this);
    }
    
    /**
     * Creates a new top level module.
     * 
     * @param name
     *      the name of the module
     * @param spec
     *      the spec
     */
    public Module(ModuleManager manager, String name, Spec spec) {
        this.manager = manager;
        this.name = name;
        this.parent = null;
        this.scopeGraph = new ModuleScopeGraph(this, spec.labels(), spec.endOfPath(), spec.relations().keySet(), Collections.emptyList());
        manager.addModule(this);
    }
    
    /**
     * Constructor for creating child modules.
     * 
     * @param name
     *      the name of the child
     * @param parent
     *      the parent module
     */
    private Module(ModuleManager manager, String name, IModule parent) {
        this.manager = manager;
        this.name = name;
        this.parent = parent;
        manager.addModule(this);
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getId() {
        return parent == null ? name : ModulePaths.build(parent.getId(), name);
    }
    
    @Override
    public IModule getParent() {
        return parent;
    }
    
    @Override
    public void setParent(IModule module) {
        this.parent = module;
    }

    @Override
    public IMInternalScopeGraph<IOwnableTerm,  ITerm, ITerm, ITerm> getScopeGraph() {
        return scopeGraph;
    }
    
    @Override
    public IMState getCurrentState() {
        return state;
    }
    
    @Override
    public void setCurrentState(IMState state) {
        if (this.state != null) System.out.println("NOTE: The state of module " + name + " is already set");
        this.state = state;
    }

    @Override
    public synchronized Module createChild(String name, List<IOwnableScope> canExtend) {
        Module child = new Module(manager, name, this);
        child.scopeGraph = scopeGraph.createChild(child, canExtend);
        return child;
    }
    
//    @Override
//    public Module copy(ModuleManager newManager, IModule newParent, List<IOwnableScope> newScopes) {
//        //TODO This needs to be changed. We might need to record the old version, to do comparisons against.
//        //TODO We also cannot instantiate our children yet. The mechanism needs to be different, based around
//        //TODO lookups OR creations.
//        Module copy = new Module(newManager, id, newParent);
//        copy.flag(ModuleCleanliness.CLIRTY);
//        copy.scopeGraph = scopeGraph.recreate(newScopes);
//        
//        //TODO We cannot really copy the children properly
//        for (IModule child : children) {
//            IModule childCopy = child.copy(newManager, newParent);
//        }
//        return copy;
//    }
    
    @Override
    public Set<IModule> getDependencies() {
        return queries.values().stream().flatMap(d -> d.getReachedModules().stream()).collect(Collectors.toSet());
    }
    
    @Override
    public void addQuery(CResolveQuery query, QueryDetails<IOwnableTerm, ITerm, ITerm> details) {
        queries.put(query, details);
    }
    
    @Override
    public void addDependant(IModule module, CResolveQuery query) {
        dependants.put(module, query);
    }
    
    @Override
    public Map<IModule, CResolveQuery> getDependants() {
        return dependants;
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
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Module)) return false;
        assert !this.getId().equals(((Module) obj).getId()) : "Module identifiers are equal but modules are not the same instance! (id: " + getId() + ")";
        return this.getId().equals(((Module) obj).getId());
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() + (parent == null ? 0 : (31 * parent.hashCode()));
    }
    
    @Override
    public String toString() {
        return "@" + getId();
    }
}
