package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Function2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
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
import mb.statix.spec.Type;
import mb.statix.spoofax.MSTX_solve_constraint;
import mb.statix.spoofax.StatixTerms;
import mb.statix.taico.scopegraph.ITrackingScopeGraph;
import mb.statix.taico.scopegraph.locking.LockManager;
import mb.statix.taico.scopegraph.reference.MFastNameResolution;
import mb.statix.taico.solver.CompletenessResult;
import mb.statix.taico.solver.IMState;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
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
public class CResolveQuery implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final @Nullable ITerm relation;
    private final IQueryFilter filter;
    private final IQueryMin min;
    private final ITerm scopeTerm;
    private final ITerm resultTerm;

    private final @Nullable IConstraint cause;

    public CResolveQuery(Optional<ITerm> relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm,
            ITerm resultTerm) {
        this(relation, filter, min, scopeTerm, resultTerm, null);
    }

    public CResolveQuery(Optional<ITerm> relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm,
            ITerm resultTerm, @Nullable IConstraint cause) {
        this.relation = relation.orElse(null);
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
        this.cause = cause;
    }

    public Optional<ITerm> relation() {
        return Optional.ofNullable(relation);
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
        final Type type;
        if(relation().isPresent()) {
            type = state.spec().relations().get(relation().get());
            if(type == null) {
                params.debug().error("Ignoring query for unknown relation {}", relation);
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
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        final IDebugContext subDebug;
        if (MSTX_solve_constraint.QUERY_DEBUG) {
            subDebug = new PrefixedDebugContext("Query", params.debug().subContext());
        } else {
            subDebug = new NullDebugContext(params.debug().getDepth() + 1);
        }
        final Function2<ITerm, ITerm, CompletenessResult> isComplete = (s, l) -> {
            CompletenessResult result = params.isComplete(s, l, state);
            if(result.delay() != null) {
                subDebug.info("Completeness of {} {} delayed on module {}", s, l, result.delay().module());
            } else if(result.isComplete()) {
                subDebug.info("{} complete in {}", s, l);
            } else {
                subDebug.info("{} incomplete in {}", s, l);
            }
            return result;
        };
        
        IMQueryFilter filter = this.filter.toMutable();
        IMQueryMin min = this.min.toMutable();
        
        ITrackingScopeGraph<AScope, ITerm, ITerm, ITerm> trackingGraph = state.scopeGraph().trackingGraph();
        final Set<IResolutionPath<ITerm, ITerm, ITerm>> paths;
        
        LockManager lockManager = new LockManager(state.owner());
        try {
            // @formatter:off
            final MFastNameResolution<AScope, ITerm, ITerm, ITerm> nameResolution = MFastNameResolution.<AScope, ITerm, ITerm, ITerm>builder()
                    .withLabelWF(filter.getLabelWF(state, params::isComplete, subDebug))
                    .withDataWF(filter(relation(), type, filter.getDataWF(state, params::isComplete, subDebug), subDebug))
                    .withLabelOrder(min.getLabelOrder(state, params::isComplete, subDebug))
                    .withDataEquiv(filter(relation(), type, min.getDataEquiv(state, params::isComplete, subDebug), subDebug))
                    .withEdgeComplete(isComplete)
                    .withDataComplete(isComplete)
                    .withLockManager(lockManager)
                    .build(state.context(), trackingGraph, relation());
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
        if(relation != null) {
            pathTerms = paths.stream().map(p -> B.newTuple(B.newBlob(p.getPath()), B.newTuple(p.getDatum())))
                    .collect(Collectors.toList());
        } else {
            pathTerms = paths.stream().map(p -> B.newBlob(p.getPath())).collect(Collectors.toList());
        }
        
        //Register this query
        QueryDetails<AScope, ITerm, ITerm> details = new QueryDetails<AScope, ITerm, ITerm>(
                trackingGraph.aggregateTrackedEdges(),
                trackingGraph.aggregateTrackedData(),
                trackingGraph.getReachedModules(),
                pathTerms);
        state.owner().addQuery(this, details);
        
        //Add reverse dependencies
        for (String module : trackingGraph.getReachedModules()) {
            state.context().getModuleUnchecked(module).addDependant(state.owner().getId(), this);
        }
        
        final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm, this);
        return Optional.of(MConstraintResult.ofConstraints(C));
    }
    
    private DataWF<ITerm> filter(Optional<ITerm> relation, Type type, DataWF<ITerm> filter, IDebugContext debug) {
        return new DataWF<ITerm>() {
            @Override public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
                return filter.wf(filter(relation, type, datum, debug));
            }
        };
    }

    private DataLeq<ITerm> filter(Optional<ITerm> relation, Type type, DataLeq<ITerm> filter, IDebugContext debug) {
        return new DataLeq<ITerm>() {

            @Override public boolean leq(List<ITerm> d1, List<ITerm> d2)
                    throws ResolutionException, InterruptedException {
                return filter.leq(filter(relation, type, d1, debug), filter(relation, type, d2, debug));
            }

            @Override public boolean alwaysTrue() throws InterruptedException {
                return filter.alwaysTrue();
            }

        };
    }

    private List<ITerm> filter(Optional<ITerm> relation, Type type, List<ITerm> datum, IDebugContext debug)
            throws ResolutionException {
        if(datum.size() != type.getArity()) {
            debug.error("Ignoring {}-ary data for {}-ary relation {}", datum.size(), type.getArity(), relation);
            throw new ResolutionException("Wrong data arity.");
        }
        return datum.stream().limit(type.getInputArity()).collect(Collectors.toList());
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CResolveQuery withCause(@Nullable IConstraint cause) {
        return new CResolveQuery(relation(), filter, min, scopeTerm, resultTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseResolveQuery(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseResolveQuery(this);
    }

    @Override public CResolveQuery apply(ISubstitution.Immutable subst) {
        return new CResolveQuery(relation(), filter.apply(subst), min.apply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("query ");
        sb.append(relation());
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
