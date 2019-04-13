package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.log.PrefixedDebugContext;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.Type;
import mb.statix.spoofax.STX_solve_constraint;
import mb.statix.spoofax.StatixTerms;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.scopegraph.ITrackingScopeGraph;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.scopegraph.locking.LockManager;
import mb.statix.taico.scopegraph.reference.MFastNameResolution;
import mb.statix.taico.solver.CompletenessResult;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.query.IMQueryFilter;
import mb.statix.taico.solver.query.IMQueryMin;
import mb.statix.taico.solver.query.QueryDetails;

/**
 * Implementation for a query constraint.
 * 
 * <pre>query [relation]
 * filter [filters]
 * min [min]
 * in [scope]
 * |-> [result]</pre>
 */
public class CResolveQuery implements IConstraint {

    private final Optional<ITerm> relation;
    private final IMQueryFilter filter;
    private final IMQueryMin min;
    private final ITerm scopeTerm;
    private final ITerm resultTerm;

    private final @Nullable IConstraint cause;

    public CResolveQuery(Optional<ITerm> relation, IMQueryFilter filter, IMQueryMin min, ITerm scopeTerm,
            ITerm resultTerm) {
        this(relation, filter, min, scopeTerm, resultTerm, null);
    }

    public CResolveQuery(Optional<ITerm> relation, IMQueryFilter filter, IMQueryMin min, ITerm scopeTerm,
            ITerm resultTerm, @Nullable IConstraint cause) {
        this.relation = relation;
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
        this.cause = cause;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CResolveQuery withCause(@Nullable IConstraint cause) {
        return new CResolveQuery(relation, filter, min, scopeTerm, resultTerm, cause);
    }

    @Override public CResolveQuery apply(ISubstitution.Immutable subst) {
        return new CResolveQuery(relation, filter.apply(subst), min.apply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause);
    }

    /**
     * @see IConstraint#solve
     * 
     * @throws IllegalArgumentException
     *      If the query is applied on a term that is not a scope.
     * @throws InterruptedException
     *      If this thread has been interrupted when the name resolution is executed.
     * @throws Delay
     *      If the scope we are querying is not ground relative to the unifier.
     * @throws Delay
     *      If the scope we are querying is incomplete in terms of relations or edges we are
     *      interested in.
     * @throws Delay
     *      If the filter throws a ResolutionDelayException from {@link IQueryFilter#getDataWF}
     *      or {@link IQueryFilter#getLabelWF}.
     * @throws Delay
     *      If the min throws a ResolutionDelayException from {@link IQueryMin#getLabelOrder}
     *      or {@link IQueryMin#getDataEquiv}.
     * @throws Delay
     *      If the resolution throws a ResolutionDelayException from
     *      {@link FastNameResolution#resolve}.
     */
    @Override
    public Optional<MConstraintResult> solve(MState state, MConstraintContext params)
            throws InterruptedException, Delay {
        final Type type;
        if(relation.isPresent()) {
            type = state.spec().relations().get(relation.get());
            if(type == null) {
                params.debug().error("Ignoring query for unknown relation {}", relation.get());
                return Optional.empty();
            }
        } else {
            type = StatixTerms.SCOPE_REL_TYPE;
        }

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            System.err.println("Delaying query on the scope of the query: (not ground) " + scopeTerm);
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final OwnableScope scope = OwnableScope.ownableMatcher(state.manager()::getModule).match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        final IDebugContext subDebug;
        if (STX_solve_constraint.QUERY_DEBUG) {
            subDebug = new PrefixedDebugContext("Query", params.debug().subContext());
        } else {
            subDebug = new NullDebugContext(params.debug().getDepth() + 1);
        }
        final Function2<ITerm, ITerm, CompletenessResult> isComplete = (s, l) -> {
            CompletenessResult result = params.completeness().isComplete(s, l, state);
            if(result.isComplete()) {
                subDebug.info("{} complete in {}", s, l);
            } else {
                subDebug.info("{} incomplete in {}", s, l);
            }
            return result;
        };
        
        ITrackingScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> trackingGraph = state.scopeGraph().trackingGraph();
        final Set<IResolutionPath<ITerm, ITerm, ITerm>> paths;
        
        LockManager lockManager = new LockManager(state.owner());
        try {
            // @formatter:off
            final MFastNameResolution<IOwnableTerm, ITerm, ITerm, ITerm> nameResolution = MFastNameResolution.<IOwnableTerm, ITerm, ITerm, ITerm>builder()
                    .withLabelWF(filter.getLabelWF(state, params.completeness(), subDebug))
                    .withDataWF(filter(type, filter.getDataWF(state, params.completeness(), subDebug), subDebug))
                    .withLabelOrder(min.getLabelOrder(state, params.completeness(), subDebug))
                    .withDataEquiv(filter(type, min.getDataEquiv(state, params.completeness(), subDebug), subDebug))
                    .withEdgeComplete(isComplete)
                    .withDataComplete(isComplete)
                    .withLockManager(lockManager)
                    .build(trackingGraph, relation);
            // @formatter:on
            
            
            paths = nameResolution.resolve(scope);
        } catch(IncompleteDataException e) {
            System.err.println("Delaying query on a (data) edge: " + e.scope() + " " + e.relation() + ": (critical edge)");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation(), e.getModule()), lockManager);
        } catch(IncompleteEdgeException e) {
            System.err.println("Delaying query on an edge: " + e.scope() + " " + e.label() + ": (critical edge)");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label(), e.getModule()), lockManager);
        } catch(ResolutionDelayException e) {
            System.err.println("Delaying query for unknown reason");
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            lockManager.absorb(e.getCause().getLockManager());
            e.getCause().setLockManager(lockManager);
            throw e.getCause();
        } catch(ResolutionException e) {
            lockManager.releaseAll();
            params.debug().info("Query resolution failed: {}", e.getMessage());
            return Optional.empty();
        } catch(Exception e) {
            lockManager.releaseAll();
            throw e;
        }
        
        //If the query was successful, we can release all locks.
        lockManager.releaseAll();
        
        final List<ITerm> pathTerms;
        if(relation.isPresent()) {
            pathTerms = paths.stream().map(p -> B.newTuple(B.newBlob(p.getPath()), B.newTuple(p.getDatum())))
                    .collect(Collectors.toList());
        } else {
            pathTerms = paths.stream().map(p -> B.newBlob(p.getPath())).collect(Collectors.toList());
        }
        
        //Register this query
        QueryDetails<IOwnableTerm, ITerm, ITerm> details = new QueryDetails<>(
                trackingGraph.aggregateTrackedEdges(),
                trackingGraph.aggregateTrackedData(),
                trackingGraph.getReachedModules(),
                pathTerms);
        state.owner().addQuery(this, details);
        
        //Add reverse dependancies
        for (IModule module : trackingGraph.getReachedModules()) {
            module.addDependant(state.owner(), this);
        }
        
        final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm, this);
        return Optional.of(MConstraintResult.ofConstraints(C));
    }

    private DataWF<ITerm> filter(Type type, DataWF<ITerm> filter, IDebugContext debug) {
        return new DataWF<ITerm>() {
            public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
                return filter.wf(filter(type, datum, debug));
            }
        };
    }

    private DataLeq<ITerm> filter(Type type, DataLeq<ITerm> filter, IDebugContext debug) {
        return new DataLeq<ITerm>() {

            public boolean leq(List<ITerm> d1, List<ITerm> d2) throws ResolutionException, InterruptedException {
                return filter.leq(filter(type, d1, debug), filter(type, d2, debug));
            }

            public boolean alwaysTrue() throws InterruptedException {
                return filter.alwaysTrue();
            }

        };
    }

    private List<ITerm> filter(Type type, List<ITerm> datum, IDebugContext debug) throws ResolutionException {
        if(datum.size() != type.getArity()) {
            debug.error("Ignoring {}-ary data for {}-ary relation {}", datum.size(), type.getArity(), relation);
            throw new ResolutionException("Wrong data arity.");
        }
        return datum.stream().limit(type.getInputArity()).collect(Collectors.toList());
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
