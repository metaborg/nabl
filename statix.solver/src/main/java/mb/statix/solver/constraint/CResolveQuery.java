package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Predicate2;

import mb.nabl2.scopegraph.terms.Scope;
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
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.Type;
import mb.statix.spoofax.StatixTerms;
import mb.statix.taico.scopegraph.IOwnableTerm;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.scopegraph.reference.MFastNameResolution;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.query.IMQueryFilter;
import mb.statix.taico.solver.query.IMQueryMin;

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
    @Override public Optional<ConstraintResult> solve(State state, ConstraintContext params)
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
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        try {
            final IDebugContext subDebug = new NullDebugContext(params.debug().getDepth() + 1);
            final Predicate2<ITerm, ITerm> isComplete = (s, l) -> {
                if(params.completeness().isComplete(s, l, state)) {
                    subDebug.info("{} complete in {}", s, l);
                    return true;
                } else {
                    subDebug.info("{} incomplete in {}", s, l);
                    return false;
                }
            };
            // @formatter:off
            final FastNameResolution<ITerm, ITerm, ITerm> nameResolution = FastNameResolution.<ITerm, ITerm, ITerm>builder()
                    .withLabelWF(filter.getLabelWF(state, params.completeness(), subDebug))
                    .withDataWF(filter(type, filter.getDataWF(state, params.completeness(), subDebug), subDebug))
                    .withLabelOrder(min.getLabelOrder(state, params.completeness(), subDebug))
                    .withDataEquiv(filter(type, min.getDataEquiv(state, params.completeness(), subDebug), subDebug))
                    .withEdgeComplete(isComplete)
                    .withDataComplete(isComplete)
                    .build(state.scopeGraph(), relation);
            // @formatter:on
            final Set<IResolutionPath<ITerm, ITerm, ITerm>> paths = nameResolution.resolve(scope);
            final List<ITerm> pathTerms;
            if(relation.isPresent()) {
                pathTerms = paths.stream().map(p -> B.newTuple(B.newBlob(p.getPath()), B.newTuple(p.getDatum())))
                        .collect(Collectors.toList());
            } else {
                pathTerms = paths.stream().map(p -> B.newBlob(p.getPath())).collect(Collectors.toList());
            }
            final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm, this);
            return Optional.of(ConstraintResult.ofConstraints(state, C));
        } catch(IncompleteDataException e) {
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation()));
        } catch(IncompleteEdgeException e) {
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label()));
        } catch(ResolutionDelayException e) {
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw e.getCause();
        } catch(ResolutionException e) {
            params.debug().info("Query resolution failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<MConstraintResult> solveMutable(MState state, MConstraintContext params)
            throws InterruptedException, Delay {
        final Type type;
        if(relation.isPresent()) {
            //TODO TAICO This might need to be moved to the scope graph instead of the spec, though since it is just a type check, it might be fine.
            type = state.spec().relations().get(relation.get());
            if(type == null) {
                params.debug().error("Ignoring query for unknown relation {}", relation.get());
                return Optional.empty();
            }
        } else {
            type = StatixTerms.SCOPE_REL_TYPE;
        }

        final IUnifier.Immutable unifier = state.unifier();
        //TODO TAICO We might be trying to query a scope that is not ours, will our unifier still call it ground?
        if(!unifier.isGround(scopeTerm)) {
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final OwnableScope scope = OwnableScope.ownableMatcher(state.manager()::getModule).match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        try {
            final IDebugContext subDebug = new NullDebugContext(params.debug().getDepth() + 1);
            final Predicate2<ITerm, ITerm> isComplete = (s, l) -> {
                if(params.completeness().isComplete(s, l, state)) {
                    subDebug.info("{} complete in {}", s, l);
                    return true;
                } else {
                    subDebug.info("{} incomplete in {}", s, l);
                    return false;
                }
            };
            IMQueryFilter filter = this.filter.toMutable();
            IMQueryMin min = this.min.toMutable();
            //TODO TAICO fix queries
            // @formatter:off
            final MFastNameResolution<IOwnableTerm, ITerm, ITerm, ITerm> nameResolution = MFastNameResolution.<IOwnableTerm, ITerm, ITerm, ITerm>builder()
                    .withLabelWF(filter.getLabelWF(state.copy(), params.completeness().copy(), subDebug))
                    .withDataWF(filter(type, filter.getDataWF(state.copy(), params.completeness().copy(), subDebug), subDebug))
                    .withLabelOrder(min.getLabelOrder(state.copy(), params.completeness().copy(), subDebug))
                    .withDataEquiv(filter(type, min.getDataEquiv(state.copy(), params.completeness().copy(), subDebug), subDebug))
                    .withEdgeComplete(isComplete)
                    .withDataComplete(isComplete)
                    .build(state.scopeGraph(), relation);
            // @formatter:on
            final Set<IResolutionPath<ITerm, ITerm, ITerm>> paths = nameResolution.resolve(scope);
            final List<ITerm> pathTerms;
            if(relation.isPresent()) {
                pathTerms = paths.stream().map(p -> B.newTuple(B.newBlob(p.getPath()), B.newTuple(p.getDatum())))
                        .collect(Collectors.toList());
            } else {
                pathTerms = paths.stream().map(p -> B.newBlob(p.getPath())).collect(Collectors.toList());
            }
            final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm, this);
            return Optional.of(MConstraintResult.ofConstraints(state, C));
        } catch(IncompleteDataException e) {
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation()));
        } catch(IncompleteEdgeException e) {
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label()));
        } catch(ResolutionDelayException e) {
            params.debug().info("Query resolution delayed: {}", e.getMessage());
            throw e.getCause();
        } catch(ResolutionException e) {
            params.debug().info("Query resolution failed: {}", e.getMessage());
            return Optional.empty();
        }
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
