package mb.statix.taico.module;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.ITerm;
import mb.statix.taico.paths.IQuery;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.scopegraph.ModuleScopeGraph;

/**
 * Basic implementation of {@link IModule}. The identifiers are not automatically generated.
 */
//TODO This would be a StatixModule or SGModule
public class Module implements IModule {
    private final String id;
    private IModule parent;
    private Set<IModule> children = new HashSet<>();
    private IMInternalScopeGraph<IOwnableScope, ITerm, ITerm> scopeGraph;
    
    
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
    public Module(String id, Immutable<ITerm> labels, ITerm endOfPath, Immutable<ITerm> relations) {
        this.id = id;
        this.parent = null;
        this.scopeGraph = new ModuleScopeGraph(this, labels, endOfPath, relations, Immutable.of());
    }
    
    public Module(String id, IModule parent) {
        this.id = id;
        this.parent = parent;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<IQuery<IOwnableScope, ITerm, ITerm>> queries() {
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
    public IMInternalScopeGraph<IOwnableScope, ITerm, ITerm> getScopeGraph() {
        return scopeGraph;
    }

    @Override
    public synchronized Module createChild(io.usethesource.capsule.Set.Immutable<IOwnableScope> canExtend) {
        final String newId = generateNewChildId();
        
        Module child = new Module(newId, this);
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
        Matcher matcher = Pattern.compile("(.*)\\_(\\d+)").matcher(id);
        
        final String newId;
        if (matcher.matches()) {
            newId = matcher.group(1) + "_" + (Integer.parseInt(matcher.group(2)) + 1);
        } else {
            newId = id + "_0";
        }
        return newId;
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
