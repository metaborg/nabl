package mb.statix.constraints;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Function2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
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
import mb.statix.taico.scopegraph.ITrackingScopeGraph;
import mb.statix.taico.scopegraph.reference.MFastNameResolution;
import mb.statix.taico.scopegraph.reference.ModuleDelayException;
import mb.statix.taico.solver.CompletenessResult;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.query.MConstraintQueries;
import mb.statix.taico.solver.query.QueryDetails;
import mb.statix.taico.util.TDebug;

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
    
    @Override
    public Optional<MConstraintResult> solve(IMState state, MConstraintContext params)
            throws InterruptedException, Delay {
        final ITerm relation = relation();
        final IQueryFilter filter = filter();
        final IQueryMin min = min();
        final ITerm scopeTerm = scopeTerm();
        final ITerm resultTerm = resultTerm();

        final IUnifier unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query on the scope of the query: (not ground) " + scopeTerm);
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final Scope scope = AScope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        final IDebugContext subDebug;
        if (TDebug.QUERY_DEBUG) {
            subDebug = new PrefixedDebugContext("Query", params.debug().subContext());
        } else {
            subDebug = new NullDebugContext(params.debug().getDepth() + 1);
        }
        final Function2<Scope, ITerm, CompletenessResult> isComplete = (s, l) -> {
            CompletenessResult result = params.isComplete(s, l, state);
            if(result.isComplete()) {
                subDebug.info("{} complete in {}", s, l);
            } else {
                subDebug.info("{} incomplete in {}", s, l);
            }
            return result;
        };

        ITrackingScopeGraph<Scope, ITerm, ITerm> trackingGraph = state.scopeGraph().trackingGraph();
        final Set<IResolutionPath<Scope, ITerm, ITerm>> paths;
        try {
            final MConstraintQueries cq = new MConstraintQueries(state, params);
            // @formatter:off
            final MFastNameResolution<Scope, ITerm, ITerm> nameResolution = MFastNameResolution.<Scope, ITerm, ITerm>builder()
                        .withLabelWF(cq.getLabelWF(filter.getLabelWF()))
                        .withDataWF(cq.getDataWF(filter.getDataWF()))
                        .withLabelOrder(cq.getLabelOrder(min.getLabelOrder()))
                        .withDataEquiv(cq.getDataEquiv(min.getDataEquiv()))
                        .withEdgeComplete(isComplete)
                        .withDataComplete(isComplete)
                        .build(trackingGraph, relation);
            // @formatter:on
            
            paths = nameResolution.resolve(scope);
        } catch(IncompleteDataException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query on a (data) edge: " + e.scope() + " " + e.relation() + ": (critical edge)");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation(), e.getModule()), trackingGraph.getLockManager());
        } catch(IncompleteEdgeException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query on an edge: " + e.scope() + " " + e.label() + ": (critical edge)");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label(), e.getModule()), trackingGraph.getLockManager());
        } catch(ResolutionDelayException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query for unknown reason");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            trackingGraph.getLockManager().absorb(e.getCause().getLockManager());
            e.getCause().setLockManager(trackingGraph.getLockManager());
            throw e.getCause();
        } catch(ModuleDelayException e) {
            if (TDebug.QUERY_DELAY) System.err.println("Delaying query on module " + e.getModule());
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofModule(e.getModule());
        } catch(ResolutionException e) {
            trackingGraph.unlockAll();
            params.debug().info("Query resolution failed: {}", e.getMessage());
            return Optional.empty();
        } catch(Exception e) {
            trackingGraph.unlockAll();
            throw e;
        }        
        
        //If the query was successful, we can release all locks.
        trackingGraph.unlockAll();
        
        final List<ITerm> pathTerms =
                paths.stream().map(StatixTerms::explicate).collect(ImmutableList.toImmutableList());
        
        //Register this query
        QueryDetails<Scope, ITerm, ITerm> details = new QueryDetails<Scope, ITerm, ITerm>(
                trackingGraph.aggregateTrackedEdges(),
                trackingGraph.aggregateTrackedData(),
                trackingGraph.getReachedModules(),
                pathTerms);
        state.owner().addQuery(this, details);
        
        //Add reverse dependencies
        for (String module : trackingGraph.getReachedModules()) {
            SolverContext.context().getModuleUnchecked(module).addDependant(state.owner().getId(), this);
        }
        
        final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm, this);
        return Optional.of(MConstraintResult.ofConstraints(C));        
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
                cause);
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

}
