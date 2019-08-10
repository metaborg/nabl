package mb.statix.taico.dependencies.details;

import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.Scope;

public class QueryResultDependencyDetail implements IDependencyDetail {
    private static final long serialVersionUID = 1L;
    
    private final Set<IResolutionPath<Scope, ITerm, ITerm>> paths;
//    private final List<ITerm> queryResults;
    
//    public QueryResultDependencyDetail(List<ITerm> queryResults, Set<IResolutionPath<Scope, ITerm, ITerm>> paths) {
//        this.queryResults = queryResults;
//        this.paths = paths;
//    }
    
    public QueryResultDependencyDetail(Set<IResolutionPath<Scope, ITerm, ITerm>> paths) {
        this.paths = paths;
    }
    
    public Set<IResolutionPath<Scope, ITerm, ITerm>> getPaths() {
        return paths;
    }

//    public List<ITerm> getQueryResults() {
//        return queryResults;
//    }
    
    // --------------------------------------------------------------------------------------------
    // Affected by changes
    // --------------------------------------------------------------------------------------------
    
    /**
     * @param scope
     *      the removed scope
     * 
     * @return
     *      true if this dependency can be affected by the removal of the scope, false otherwise
     */
    public boolean isAffectedByScopeRemoval(Scope scope) {
        for (IResolutionPath<Scope, ITerm, ITerm> path : paths) {
            if (path.scopeSet().contains(scope)) return true;
        }
        
        return false;
    }
    
    /**
     * Determines if this query is affected by the removal of the given edge.
     * <p>
     * If the resolution paths of this dependency contains the given scope, then this method uses
     * a linear search to determine if the given edge is on the path.
     * 
     * TODO OPTIMIZATION We might want to build the set of edges beforehand and avoid the linear search?
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
        //If any resolution path contains this scope and label, we are affected
        for (IResolutionPath<Scope, ITerm, ITerm> path : paths) {
            System.out.println("Checking " + path + " if it is affected by " + scope + " -" + label + "->");
            
            //NOTE: The scope set is lazily computed
            if (!path.scopeSet().contains(scope)) continue;
            for (IStep<Scope, ITerm> step : path.getPath()) {
                if (step.getSource().equals(scope) && step.getLabel().equals(label)) return true;
            }
        }
        return false;
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
        for (IResolutionPath<Scope, ITerm, ITerm> path : paths) {
            System.out.println("Checking " + path + " if it is affected by " + scope + " -" + relation + "-[]");
            
            if (!scope.equals(path.getPath().getTarget())) continue;
            if (relation.equals(path.getLabel())) return true;
        }
        return false;
    }
}
