package mb.statix.taico.dependencies.details;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.MaybeNotInstantiatedBool;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CResolveQuery.QueryResult;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.taico.scopegraph.reference.TrackingNameResolution;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.util.Modules;
import mb.statix.taico.util.TPrettyPrinter;

public class QueryDependencyDetail implements IDependencyDetail {
    //TODO OPTIMIZATION optimize this factor if it is significant
    private static final int PATH_LENGTH_CHECK = 10;

    private static final long serialVersionUID = 1L;
    
    private final String owner;
    private final CResolveQuery constraint;
    private final Multimap<Scope, LabelWF<ITerm>> edges;
    private final Multimap<Scope, LabelWF<ITerm>> data;
    private final List<ITerm> queryResult;
    private Set<String> modules;
    private final Set<IResolutionPath<Scope, ITerm, ITerm>> paths;

    public QueryDependencyDetail(String owner, CResolveQuery constraint, TrackingNameResolution<Scope, ITerm, ITerm> nameResolution,
            List<ITerm> queryResult, Set<IResolutionPath<Scope, ITerm, ITerm>> paths) {
        this(owner, constraint, nameResolution.getTrackedEdges(), nameResolution.getTrackedData(), queryResult, paths);
    }

    public QueryDependencyDetail(String owner, CResolveQuery constraint, Multimap<Scope, LabelWF<ITerm>> edges,
            Multimap<Scope, LabelWF<ITerm>> data, List<ITerm> queryResult, Set<IResolutionPath<Scope, ITerm, ITerm>> paths) {
        this.owner = owner;
        this.constraint = constraint;
        this.edges = edges;
        this.data = data;
        this.queryResult = queryResult;
        this.modules = computeModules(owner);
        this.paths = paths;
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

    public List<ITerm> getQueryResult() {
        return queryResult;
    }
    
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
        if (!edges.containsKey(scope) && !data.containsKey(scope)) return false;
        
        for (IResolutionPath<Scope, ITerm, ITerm> path : paths) {
            //Lazily computed set, only beneficial if multiple checks are made
            if (path.scopeSet().contains(scope)) return true;
        }
        
        return false;
    }
    
    /**
     * Determines if this dependency is affected by the addition of the given edge.
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
        return checkAffectedByAddition(scope, label, collection);
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
        if (!edges.containsKey(scope)) return false;
        
        //If any resolution path contains this scope and label, we are affected
        for (IResolutionPath<Scope, ITerm, ITerm> path : paths) {
            //Only check the scopes if the path length is above a certain number. The scope set is lazily computed
            if (path.getPath().size() > PATH_LENGTH_CHECK && !path.scopeSet().contains(scope)) continue;
            for (IStep<Scope, ITerm> step : path.getPath()) {
                if (step.getSource().equals(scope) && step.getLabel().equals(label)) return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if this dependency is affected by the addition of the given edge.
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
        return checkAffectedByAddition(scope, relation, collection);
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
            if (!scope.equals(path.getPath().getTarget())) continue;
            if (relation.equals(path.getLabel())) return true;
        }
        return false;
    }

    /**
     * Checks if this dependency is affected by the addition of the given edge/data edge.
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
    private boolean checkAffectedByAddition(Scope scope, ITerm label, Collection<LabelWF<ITerm>> collection) {
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
    // Redoing query
    // --------------------------------------------------------------------------------------------
    
    public CResolveQuery recreateQuery(Scope scope, LabelWF<ITerm> labelWf, IMState state) {
        //TODO We will create multiple separate queries, how to handle this?
        IQueryFilter filter = new QueryFilter(((RegExpLabelWF) labelWf).getRegex(), constraint.filter().getDataWF());
        return new CResolveQuery(constraint.relation(), filter, constraint.min(), scope, constraint.resultTerm());
    }
    
    public QueryResult redo(Scope scope, LabelWF<ITerm> labelWf, IMState state, MConstraintContext params) throws Delay, InterruptedException {
        CResolveQuery query = recreateQuery(scope, labelWf, state);
        //To redo a query properly, we need a new state, and we need the information of the old query. We use the new regex (derivative) and we should be able to use all the other information
        //from the original query.
        //TODO IMPORTANT Be able to actually create/run derivative queries. A derivative query has a different scope term and label wellformedness.
        return query.resolveQuery(state, params);
    }
    
    public QueryResultComparison isResultSame(List<ITerm> newResults) {
        int nSize = newResults.size();
        int oSize = queryResult.size();
        if (oSize != nSize) return QueryResultComparison.DIFFERENT_COUNT;
        
        IMState state = Modules.moduleUnchecked(owner).getCurrentState();
        for (int i = 0; i < nSize; i++) {
            ITerm oldTerm = queryResult.get(i);
            ITerm newTerm = newResults.get(i);
            MaybeNotInstantiatedBool result = state.unifier().areEqual(oldTerm, newTerm);
            if (!result.orElse(false)) {
                if (result.orElse(true)) {
                    //Our boolean is returned unchanged, so there were variables left that are not instantiated
                    return QueryResultComparison.UNBOUND_VARS;
                } else {
                    //result is false, so the terms are different
                    return QueryResultComparison.DIFFERENT_TERMS;
                }
            }
        }
        
        return QueryResultComparison.UNIFIED_SAME;
    }
    
    /**
     * Enum to signal different comparison results.
     */
    public static enum QueryResultComparison {
        IDENTICAL(true),
        UNIFIED_SAME(true),
        DIFFERENT_COUNT(false),
        DIFFERENT_TERMS(false),
        UNBOUND_VARS(false);
        
        private final boolean equal;
        
        private QueryResultComparison(boolean equal) {
            this.equal = equal;
        }
        
        public boolean isEqual() {
            return equal;
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "QueryDependencyDetails<edges=" + edges + ", data=" + data + ", constraint=" + constraint + ">";
    }
}
