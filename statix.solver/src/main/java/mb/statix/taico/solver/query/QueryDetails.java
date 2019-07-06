package mb.statix.taico.solver.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.MaybeNotInstantiatedBool;
import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spoofax.StatixTerms;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.scopegraph.reference.TrackingNameResolution;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.util.TDebug;

public class QueryDetails implements IQueryDetails<Scope, ITerm, ITerm> {
    private static final long serialVersionUID = 1L;

    //TODO Needs to use the state of the module
    private CResolveQuery constraint;
    private Multimap<Scope, LabelWF<ITerm>> edges;
    private Multimap<Scope, LabelWF<ITerm>> data;
    private List<ITerm> queryResult;
    private Set<String> modules;
    private String owner;

    public QueryDetails(String owner, CResolveQuery constraint, TrackingNameResolution<Scope, ITerm, ITerm> nameResolution,
            List<ITerm> queryResult) {
        this(owner, constraint, nameResolution.getTrackedEdges(), nameResolution.getTrackedData(),
                queryResult);
    }

    public QueryDetails(String owner, CResolveQuery constraint, Multimap<Scope, LabelWF<ITerm>> edges,
            Multimap<Scope, LabelWF<ITerm>> data, List<ITerm> queryResult) {
        this.owner = owner;
        this.constraint = constraint;
        this.edges = edges;
        this.data = data;
        this.queryResult = queryResult;
        this.modules = computeModules(owner);
    }

    private Set<String> computeModules(String owner) {
        Set<String> modules = new HashSet<>();
        edges.keySet().stream().map(Scope::getResource).forEach(modules::add);
        data.keySet().stream().map(Scope::getResource).forEach(modules::add);
        modules.remove(owner);
        return modules;
    }

    @Override
    public CResolveQuery getOriginalConstraint() {
        return constraint;
    }

    @Override
    public Multimap<Scope, LabelWF<ITerm>> getRelevantEdges() {
        return edges;
    }

    @Override
    public Multimap<Scope, LabelWF<ITerm>> getRelevantData() {
        return data;
    }

    @Override
    public Set<String> getReachedModules() {
        return modules;
    }

    @Override
    public List<ITerm> getQueryResult() {
        return queryResult;
    }
    
    public CResolveQuery recreateQuery(Scope scope, LabelWF<ITerm> labelWf, IMState state, MConstraintContext params) {
        //TODO We will create multiple separate queries, how to handle this?
        IQueryFilter filter = new QueryFilter(((RegExpLabelWF) labelWf).getRegex(), constraint.filter().getDataWF());
        return new CResolveQuery(constraint.relation(), filter, constraint.min(), scope, constraint.resultTerm());
    }
    
    public List<ITerm> redo(Scope scope, LabelWF<ITerm> labelWf, IMState state, MConstraintContext params) throws Delay, InterruptedException {
        //To redo a query properly, we need a new state, and we need the information of the old query. We use the new regex (derivative) and we should be able to use all the other information
        //from the original query.
        //TODO IMPORTANT Be able to actually create/run derivative queries. A derivative query has a different scope term and label wellformedness.
        IQueryFilter filter = constraint.filter();
        IQueryMin min = constraint.min();
        ITerm relation = constraint.relation();
        
        final Predicate2<Scope, ITerm> isComplete = (s, l) -> params.isComplete(s, l, state);
        
        final TrackingNameResolution<Scope, ITerm, ITerm> nameResolution;
        final Set<IResolutionPath<Scope, ITerm, ITerm>> paths;
        try {
            final MConstraintQueries cq = new MConstraintQueries(state, params);
            // @formatter:off
            nameResolution = TrackingNameResolution.<Scope, ITerm, ITerm>builder()
                        .withLabelWF(labelWf)
                        .withDataWF(cq.getDataWF(filter.getDataWF()))
                        .withLabelOrder(cq.getLabelOrder(min.getLabelOrder()))
                        .withDataEquiv(cq.getDataEquiv(min.getDataEquiv()))
                        .withEdgeComplete(isComplete)
                        .withDataComplete(isComplete)
                        .build(state.scopeGraph(), relation);
            // @formatter:on
            
            paths = nameResolution.resolve(scope);
        } catch(IncompleteDataException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query on a (data) edge: " + e.scope() + " " + e.relation() + ": (critical edge)");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation()));
        } catch(IncompleteEdgeException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query on an edge: " + e.scope() + " " + e.label() + ": (critical edge)");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label()));
        } catch(ResolutionDelayException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query for unknown reason");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw e.getCause();
        } catch(ModuleDelayException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query on module " + e.getModule());
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofModule(e.getModule());
        } catch(ResolutionException e) {
            params.debug().info("Query resolution failed: {}", e.getMessage());
            return null;
        }
        
        final List<ITerm> pathTerms =
                paths.stream().map(StatixTerms::explicate).collect(ImmutableList.toImmutableList());
        
        //We don't need to do registration or dependency addition, since we are redoing a query
//        //Register this query
//        QueryDetails<Scope, ITerm, ITerm> details = new QueryDetails<>(
//                state.owner().getId(), this, nameResolution, pathTerms, Scope::getResource);
//        state.owner().addQuery(this, details);
//        
//        //Add reverse dependencies
//        for (String module : details.getReachedModules()) {
//            SolverContext.context().getModuleUnchecked(module).addDependant(state.owner().getId(), this);
//        }
        
        return pathTerms;
    }
    
    public QueryResultComparison isResultSame(List<ITerm> newResults) {
        int nSize = newResults.size();
        int oSize = queryResult.size();
        if (oSize != nSize) return QueryResultComparison.DIFFERENT_COUNT;
        
        IMState state = SolverContext.context().getModuleUnchecked(owner).getCurrentState();
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

    @Override
    public String toString() {
        return "QueryDetails<edges=" + edges + ", data=" + data + ", constraint=" + constraint + ">";
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
}
