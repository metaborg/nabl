package mb.statix.constraints;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.modular.dependencies.details.IDependencyDetail;
import mb.statix.modular.dependencies.details.NameDependencyDetail;
import mb.statix.modular.dependencies.details.QueryDependencyDetail;
import mb.statix.modular.dependencies.details.QueryResultDependencyDetail;
import mb.statix.modular.module.IModule;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.modular.name.Names;
import mb.statix.modular.scopegraph.reference.ModuleDelayException;
import mb.statix.modular.scopegraph.reference.TrackingNameResolution;
import mb.statix.modular.solver.MConstraintContext;
import mb.statix.modular.solver.MConstraintResult;
import mb.statix.modular.solver.query.MConstraintQueries;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.util.TDebug;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spoofax.StatixTerms;

/**
 * Implementation for a query constraint.
 * 
 * <pre>query [relation]
 * filter [filters]
 * min [min]
 * in [scope]
 * |-> [result]</pre>
 */
public class CResolveQuery implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm relation;
    private final IQueryFilter filter;
    private final IQueryMin min;
    private final ITerm scopeTerm;
    private final ITerm resultTerm;

    private final @Nullable NameAndRelation name;
    private final @Nullable IConstraint cause;

    public CResolveQuery(ITerm relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm, ITerm resultTerm) {
        this(relation, filter, min, scopeTerm, resultTerm, null);
    }

    public CResolveQuery(ITerm relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm, ITerm resultTerm,
            @Nullable IConstraint cause) {
        this.relation = relation;
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
        this.cause = cause;
        this.name = Names.getMatchedName(filter.getDataWF(), relation).orElse(null);
    }
    
    private CResolveQuery(ITerm relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm, ITerm resultTerm,
            @Nullable NameAndRelation name, @Nullable IConstraint cause) {
        this.relation = relation;
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
        this.name = name;
        this.cause = cause;
    }

    public ITerm relation() {
        return relation;
    }

    public IQueryFilter filter() {
        return filter;
    }

    public IQueryMin min() {
        return min;
    }

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm resultTerm() {
        return resultTerm;
    }
    
    public NameAndRelation name() {
        return name;
    }
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params)
            throws InterruptedException, Delay {
        QueryResult result = resolveQuery(state, params);
        if (result == null) return Optional.empty();
        
        createDependencies(state.owner(), result);
        
        final IConstraint C = new CEqual(B.newList(result.pathTerms), resultTerm, this);
        return Optional.of(MConstraintResult.ofConstraints(C));        
    }
    
    /**
     * Resolves the given query by determining the paths and terms in the scope graph that match.
     * <p>
     * If query resolution fails completely, this method will return null.
     * 
     * @param state
     *      the state
     * @param params
     *      the params
     * 
     * @return
     *      the query result, or null if the resolution failed
     * 
     * @throws InterruptedException
     *      If query resolution gets interrupted.
     * @throws Delay
     *      If the scope in which the query is performed is not ground relative to the unifier in
     *      the given state (see {@link #getScope(IUnifier)}).
     * @throws Delay
     *      If the name that is requested by this query, if any, is not ground relative to the
     *      unifier in the given state.
     * @throws Delay
     *      If query resolution is delayed on an incomplete edge, incomplete data, a module or
     *      otherwise cannot be completed at this moment in time.
     */
    public QueryResult resolveQuery(IMState state, MConstraintContext params)
            throws InterruptedException, Delay {
        final IUnifier unifier = state.unifier();
        final Scope scope = getScope(unifier);
        
        NameAndRelation name = null;
        if (this.name != null) {
            name = this.name.ground(state.unifier());
        }

        final IDebugContext subDebug;
        if (TDebug.QUERY_DEBUG) {
            subDebug = new PrefixedDebugContext("Query", params.debug().subContext());
        } else {
            subDebug = new NullDebugContext(params.debug().getDepth() + 1);
        }
        
        final Predicate2<Scope, ITerm> isComplete = (s, l) -> {
            if(params.isComplete(s, l, state)) {
                subDebug.info("{} complete in {}", s, l);
                return true;
            } else {
                subDebug.info("{} incomplete in {}", s, l);
                return false;
            }
        };
        
        final TrackingNameResolution<Scope, ITerm, ITerm> nameResolution;
        final Set<IResolutionPath<Scope, ITerm, ITerm>> paths;
        try {
            final MConstraintQueries cq = new MConstraintQueries(state, params);
            // @formatter:off
            nameResolution = TrackingNameResolution.<Scope, ITerm, ITerm>builder()
                        .withLabelWF(cq.getLabelWF(filter.getLabelWF()))
                        .withDataWF(cq.getDataWF(filter.getDataWF()))
                        .withLabelOrder(cq.getLabelOrder(min.getLabelOrder()))
                        .withDataEquiv(cq.getDataEquiv(min.getDataEquiv()))
                        .withEdgeComplete(isComplete)
                        .withDataComplete(isComplete)
                        .withRequester(state.resource())
                        .withScopeToModule(Scope::getResource)
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
        return new QueryResult(name, pathTerms, paths, nameResolution);
    }
    
    /**
     * Gets the scope in which this query is performed. If the scope of this query is not ground
     * relative to the given unifier, this method throws a delay exception.
     * 
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the scope
     * 
     * @throws Delay
     *      If the scope is not ground relative to the given unifier.
     */
    public Scope getScope(IUnifier unifier) throws Delay {
        try {
            if(!unifier.isGround(scopeTerm)) {
                if (TDebug.QUERY_DELAY) System.err.println("Delaying query on the scope of the query: (not ground) " + scopeTerm);
                throw Delay.ofVars(unifier.getVars(scopeTerm));
            }
            final Scope scope = AScope.matcher().match(scopeTerm, unifier)
                    .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));
            
            return scope;
        } catch (ModuleDelayException ex) {
            throw Delay.ofModule(ex.getModule());
        }
    }

    /**
     * Creates the dependencies for this query with the given query result.
     * 
     * @param module
     *      the module which executed the query (owner of the state)
     * @param result
     *      the result of the query
     */
    public void createDependencies(IModule module, QueryResult result) {
        QueryDependencyDetail queryDetail = new QueryDependencyDetail(
                module.getId(), this, result.edges, result.data);
        QueryResultDependencyDetail resultDetail = new QueryResultDependencyDetail(result.paths);
        IDependencyDetail[] details;
        
        final NameAndRelation name = result.name;
        if (name == null) {
            details = new IDependencyDetail[2];
        } else {
            details = new IDependencyDetail[3];
            details[2] = new NameDependencyDetail(name, name.getRelation());
        }
        details[0] = queryDetail;
        details[1] = resultDetail;
        
        module.dependencies().addMultiDependency(queryDetail.getReachedModules(), details);
//        //Add reverse dependencies
//        for (String module : details.getReachedModules()) {
//            //TODO We used to have a single dependency with all the modules introduced by that dependency, we no longer have that
//            state.owner().dependencies().addDependency(module, details);
//            //TODO Make the dependant have the query details instead of the query
//            moduleUnchecked(module).addDependant(state.owner().getId(), this);
//        }
    }
    
    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CResolveQuery withCause(@Nullable IConstraint cause) {
        return new CResolveQuery(relation, filter, min, scopeTerm, resultTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseResolveQuery(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseResolveQuery(this);
    }

    @Override public CResolveQuery apply(ISubstitution.Immutable subst) {
        return new CResolveQuery(relation, filter.apply(subst), min.apply(subst), subst.apply(scopeTerm), subst.apply(resultTerm),
                name, cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("query ");
        sb.append(relation);
        sb.append(" ");
        sb.append(filter.toString(termToString));
        sb.append(" ");
        sb.append(min.toString(termToString));
        sb.append(" in ");
        sb.append(termToString.format(scopeTerm));
        sb.append(" |-> ");
        sb.append(termToString.format(resultTerm));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    /**
     * Class to represent the result of a query.
     */
    public static class QueryResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final NameAndRelation name;
        public final List<ITerm> pathTerms;
        public final Set<IResolutionPath<Scope, ITerm, ITerm>> paths;
        public final Multimap<Scope, LabelWF<ITerm>> edges;
        public final Multimap<Scope, LabelWF<ITerm>> data;
        
        public QueryResult(
                NameAndRelation name, List<ITerm> pathTerms,
                Set<IResolutionPath<Scope, ITerm, ITerm>> paths,
                TrackingNameResolution<Scope, ITerm, ITerm> nameResolution) {
            this.name = name;
            this.pathTerms = pathTerms;
            this.paths = paths;
            this.edges = nameResolution.getTrackedEdges();
            this.data = nameResolution.getTrackedData();
        }
    }
}
