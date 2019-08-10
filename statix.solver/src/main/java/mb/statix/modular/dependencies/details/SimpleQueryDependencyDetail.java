package mb.statix.modular.dependencies.details;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.terms.Scope;

public class SimpleQueryDependencyDetail implements IDependencyDetail {
    private static final long serialVersionUID = 1L;
    
    private final CResolveQuery constraint;
    private final Multimap<Scope, ITerm> edges;
    private final Multimap<Scope, ITerm> data;
    private Set<String> modules;

    public SimpleQueryDependencyDetail(String owner, CResolveQuery constraint, Multimap<Scope, ITerm> edges,
            Multimap<Scope, ITerm> data) {
        this.constraint = constraint;
        this.edges = edges;
        this.data = data;
        this.modules = computeModules(owner);
    }

    private Set<String> computeModules(String owner) {
        Set<String> modules = new HashSet<>();
        edges.keySet().stream().map(Scope::getResource).forEach(modules::add);
        data.keySet().stream().map(Scope::getResource).forEach(modules::add);
        modules.remove(owner);
        return modules;
    }
    
    // --------------------------------------------------------------------------------------------
    // Getters
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      a set with all the scopes that were visited by the query
     */
    public Set<Scope> getVisitedScopes() {
        return Sets.union(edges.keySet(), data.keySet());
    }
    
    public Set<Scope> getDataScopes() {
        return data.keySet();
    }

    public CResolveQuery getOriginalConstraint() {
        return constraint;
    }

    public Multimap<Scope, ITerm> getRelevantEdges() {
        return edges;
    }

    public Multimap<Scope, ITerm> getRelevantData() {
        return data;
    }

    public Set<String> getReachedModules() {
        return modules;
    }
    
    // --------------------------------------------------------------------------------------------
    // Affected by changes
    // --------------------------------------------------------------------------------------------
    
    /**
     * Determines if this dependency can be affected by the removal of the given scope.
     * <p>
     * Performance of this method is O(1).
     * 
     * @param scope
     *      the removed scope
     * 
     * @return
     *      true if this dependency can be affected by the removal of the scope, false otherwise
     */
    public boolean canBeAffectedByScopeRemoval(Scope scope) {
        return edges.containsKey(scope) || data.containsKey(scope);
    }
    
    /**
     * Determines if this dependency is affected by the addition of the given edge.
     * <p>
     * Performance of this method is O(1).
     * 
     * @param scope
     *      the scope of the edge (source)
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the addition of the edge, false otherwise
     */
    public boolean isAffectedByEdgeAddition(Scope scope, ITerm label) {
        return edges.containsEntry(scope, label);
    }
    
    /**
     * Determines if this query is affected by the removal of the given edge.
     * <p>
     * Performance of this method is O(1).
     * 
     * @param scope
     *      the (source) scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency is affected by the removal of the edge, false otherwise
     */
    public boolean isAffectedByEdgeRemoval(Scope scope, ITerm label) {
        return edges.containsEntry(scope, label);
    }
    
    /**
     * Determines if this dependency is affected by the addition of the given edge.
     * <p>
     * Performance of this method is O(1).
     * 
     * @param scope
     *      the scope of the edge (source)
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the addition of the edge, false otherwise
     */
    public boolean isAffectedByDataAddition(Scope scope, ITerm relation) {
        return data.containsEntry(scope, relation);
    }
    
    /**
     * Determines if this query is affected by the removal of the given data edge.
     * 
     * @param scope
     *      the (source) scope of the data edge
     * @param relation
     *      the relation of the data
     * 
     * @return
     *      true if this dependency is affected by the removal of the data edge, false otherwise
     */
    public boolean isAffectedByDataRemoval(Scope scope, ITerm relation) {
        return data.containsEntry(scope, relation);
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "SimpleQueryDependencyDetail<edges=" + edges + ", data=" + data + ">";
    }
}
