package mb.statix.modular.dependencies.details;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.modular.scopegraph.reference.TrackingNameResolution;
import mb.statix.modular.util.TPrettyPrinter;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;

public class QueryDependencyDetail implements IDependencyDetail {
    private static final long serialVersionUID = 1L;
    
    private final CResolveQuery constraint;
    private final Multimap<Scope, LabelWF<ITerm>> edges;
    private final Multimap<Scope, LabelWF<ITerm>> data;
    private Set<String> modules;

    public QueryDependencyDetail(String owner, CResolveQuery constraint, TrackingNameResolution<Scope, ITerm, ITerm> nameResolution) {
        this(owner, constraint, nameResolution.getTrackedEdges(), nameResolution.getTrackedData());
    }

    public QueryDependencyDetail(String owner, CResolveQuery constraint, Multimap<Scope, LabelWF<ITerm>> edges,
            Multimap<Scope, LabelWF<ITerm>> data) {
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

    public Multimap<Scope, LabelWF<ITerm>> getRelevantEdges() {
        return edges;
    }

    public Multimap<Scope, LabelWF<ITerm>> getRelevantData() {
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
     *      the scope to check
     * 
     * @return
     *      true if this dependency can be affected by the removal, false otherwise
     */
    public boolean canBeAffectedByScopeRemoval(Scope scope) {
        return edges.containsKey(scope) || data.containsKey(scope);
    }
    
    /**
     * Determines if this dependency can be affected by the addition of the given edge.
     * <p>
     * Expected performance of this method is equal to that of checking if a step in a regex is
     * allowed, and is thus expected to be good.
     * 
     * @param scope
     *      the scope of the edge (source)
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the addition of the edge, false otherwise
     */
    public boolean canBeAffectedByEdgeAddition(Scope scope, ITerm label) {
        Collection<LabelWF<ITerm>> collection = edges.get(scope);
        return checkAffected(scope, label, collection);
    }
    
    /**
     * Determines if this query can be affected by the removal of the given edge.
     * <p>
     * Expected performance of this method is equal to that of checking if a step in a regex is
     * allowed, and is thus expected to be good.
     * 
     * @param scope
     *      the (source) scope of the edge
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the removal of the edge, false otherwise
     */
    public boolean canBeAffectedByEdgeRemoval(Scope scope, ITerm label) {
        Collection<LabelWF<ITerm>> collection = edges.get(scope);
        return checkAffected(scope, label, collection);
    }
    
    /**
     * Determines if this dependency can be affected by the addition of the given data edge.
     * <p>
     * Expected performance of this method is equal to that of checking if a step in a regex is
     * allowed, and is thus expected to be good.
     * 
     * @param scope
     *      the scope of the edge (source)
     * @param label
     *      the label of the edge
     * 
     * @return
     *      true if this dependency can be affected by the addition of the edge, false otherwise
     */
    public boolean canBeAffectedByDataAddition(Scope scope, ITerm relation) {
        Collection<LabelWF<ITerm>> collection = data.get(scope);
        return checkAffected(scope, relation, collection);
    }
    
    /**
     * Determines if this query can be affected by the removal of the given data edge.
     * 
     * @param scope
     *      the (source) scope of the data edge
     * @param relation
     *      the relation of the data
     * 
     * @return
     *      true if this dependency can be affected by the removal of the data edge, false
     *      otherwise
     */
    public boolean canBeAffectedByDataRemoval(Scope scope, ITerm relation) {
        Collection<LabelWF<ITerm>> collection = data.get(scope);
        return checkAffected(scope, relation, collection);
    }

    /**
     * Checks if this dependency can be affected by the addition/removal of the given edge/data
     * edge.
     * 
     * @param scope
     *      the (source) scope of the edge
     * @param label
     *      the label/relation of the edge
     * @param collection
     *      the collection to look up edges in (edges or data edges)
     * 
     * @return
     *      true if this dependency can be affected, false otherwise
     */
    private boolean checkAffected(Scope scope, ITerm label, Collection<LabelWF<ITerm>> collection) {
        if (collection.isEmpty()) {
            //We did not visit this scope, ignore it
            return false;
        }
        
        for (LabelWF<ITerm> labelWf : collection) {
            try {
                if (labelWf.canStep(label)) return true;
            } catch (ResolutionException e) {
                System.err.println(
                        "Encountered delay while checking if the addition of "
                                + TPrettyPrinter.printEdge(scope, label)
                                + " affects " + constraint);
                System.err.println("Because we cannot determine for certain, returing true");
                return true;
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        System.err.println("Not affected by addition of " + TPrettyPrinter.printEdge(scope, label) + " : " + constraint);
        return false;
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "QueryDependencyDetail<edges=" + edges + ", data=" + data + ">";
    }
}
