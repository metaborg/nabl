package mb.statix.taico.module;

import java.util.HashSet;
import java.util.Set;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.ITerm;
import mb.statix.spec.Spec;
import mb.statix.taico.paths.IQuery;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.scopegraph.ModuleScopeGraph;
import mb.statix.taico.solver.MState;

/**
 * Basic implementation of {@link IModule}. The identifiers are not automatically generated.
 */
//TODO This would be a StatixModule or SGModule
public class Module implements IModule {
    private final String id;
    private final ModuleManager manager;
    private IModule parent;
    private Set<IModule> children = new HashSet<>();
    private IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> scopeGraph;
    private MState state;
    
    /**
     * Creates a new top level module.
     * 
     * @param id
     *      the id of the module
     * @param labels
     *      the labels on edges of the scope graph
     * @param endOfPath
     *      the label that indicates the end of a path
     * @param relations
     *      the labels on data edges of the scope graph
     */
    public Module(ModuleManager manager, String id, Iterable<ITerm> labels, ITerm endOfPath, Iterable<ITerm> relations) {
        this.manager = manager;
        this.id = id;
        this.parent = null;
        this.scopeGraph = new ModuleScopeGraph(this, labels, endOfPath, relations, Immutable.of());
        manager.addModule(this);
    }
    
    /**
     * Creates a new top level module.
     * 
     * @param id
     *      the id of the module
     * @param spec
     *      the spec
     */
    public Module(ModuleManager manager, String id, Spec spec) {
        this.manager = manager;
        this.id = id;
        this.parent = null;
        this.scopeGraph = new ModuleScopeGraph(this, spec.labels(), spec.endOfPath(), spec.relations().keySet(), Immutable.of());
        manager.addModule(this);
    }
    
    /**
     * Constructor for creating child modules.
     * 
     * @param id
     *      the id of the child
     * @param parent
     *      the parent module
     */
    private Module(ModuleManager manager, String id, IModule parent) {
        this.manager = manager;
        this.id = id;
        this.parent = parent;
        manager.addModule(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<IQuery<IOwnableTerm, ITerm, ITerm, ITerm>> queries() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IModule getParent() {
        return parent;
    }
    
    //TODO There should be some way to hang this module under a different module (e.g. change the parent).

    @Override
    public Set<IModule> getChildren() {
        return children;
    }

    @Override
    public IMInternalScopeGraph<IOwnableTerm,  ITerm, ITerm, ITerm> getScopeGraph() {
        return scopeGraph;
    }
    
    @Override
    public MState getCurrentState() {
        return state;
    }
    
    @Override
    public void setCurrentState(MState state) {
        if (this.state != null) throw new IllegalStateException("The state of module " + id + " is already set");
        this.state = state;
    }

    @Override
    public synchronized Module createChild(io.usethesource.capsule.Set.Immutable<IOwnableScope> canExtend) {
        final String newId = generateNewChildId();
        
        Module child = new Module(manager, newId, this);
        child.scopeGraph = new ModuleScopeGraph(child, scopeGraph.getLabels(), scopeGraph.getEndOfPath(), scopeGraph.getRelations(), canExtend);
        children.add(child);
        return child;
    }

    /**
     * Generates a new identifier for a child of this module.
     * 
     * @return
     *      the new identifier
     */
    protected String generateNewChildId() {
        return id + "_" + children.size();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Module)) return false;
        assert !this.id.equals(((Module) obj).id) : "Module identifiers are equal but modules are not the same instance! (id: " + id + ")";
        return this.id.equals(((Module) obj).id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "Module<" + id + ">";
    }

}
