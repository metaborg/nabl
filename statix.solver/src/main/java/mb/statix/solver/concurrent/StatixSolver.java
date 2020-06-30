package mb.statix.solver.concurrent;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.constraints.Constraints.disjoin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.CheckedAction0;
import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.Tuple2;
import mb.statix.actors.futures.CompletableFuture;
import mb.statix.actors.futures.IFuture;
import mb.statix.constraints.CArith;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.IState;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.ITermProperty.Multiplicity;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.persistent.BagTermProperty;
import mb.statix.solver.persistent.SingletonTermProperty;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.query.ConstraintQueries;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.solver.store.BaseConstraintStore;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

public class StatixSolver {

    private static final int MAX_DEPTH = 32;

    private final Spec spec;
    private final IConstraintStore constraints;
    private final IDebugContext debug;
    private final IProgress progress;
    private final ICancel cancel;
    private final IScopeGraphFacade<Scope, ITerm, ITerm> scopeGraph;

    private IState.Immutable state;
    private ICompleteness.Immutable completeness;
    private Map<ITermVar, ITermVar> existentials = null;
    private final List<ITermVar> updatedVars = Lists.newArrayList();
    private final Map<IConstraint, IMessage> failed = Maps.newHashMap();

    private final AtomicInteger ephemeralActiveConstraints = new AtomicInteger(0);
    private final CompletableFuture<SolverResult> result;


    public StatixSolver(String resource, IConstraint constraint, Spec spec, IDebugContext debug, IProgress progress,
            ICancel cancel, IScopeGraphFacade<Scope, ITerm, ITerm> scopeGraph) {
        this(constraint, spec, mb.statix.solver.persistent.State.of(spec).withResource(resource),
                Completeness.Immutable.of(spec), debug, progress, cancel, scopeGraph);
    }

    public StatixSolver(IConstraint constraint, Spec spec, IState.Immutable state, ICompleteness.Immutable completeness,
            IDebugContext debug, IProgress progress, ICancel cancel,
            IScopeGraphFacade<Scope, ITerm, ITerm> scopeGraph) {
        this.spec = spec;
        this.constraints = new BaseConstraintStore(debug);
        this.constraints.add(constraint);
        this.debug = debug;
        this.progress = progress;
        this.cancel = cancel;
        this.scopeGraph = scopeGraph;
        this.state = state;
        final ICompleteness.Transient _completeness = completeness.melt();
        _completeness.add(constraint, this.state.unifier());
        this.completeness = _completeness.freeze();
        this.result = new CompletableFuture<>();
    }

    ///////////////////////////////////////////////////////////////////////////
    // driver
    ///////////////////////////////////////////////////////////////////////////

    public IFuture<SolverResult> solve(Scope root) {
        try {
            scopeGraph.openRootEdges(root, getOpenEdges(root));
            fixedpoint();
        } catch(Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    public IFuture<SolverResult> entail() {
        try {
            fixedpoint();
        } catch(Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private <R> void solveK(K<R> k, R r, Throwable ex) {
        debug.info("Solving continuation");
        try {
            k.k(r, ex, MAX_DEPTH);
            fixedpoint();
        } catch(Throwable e) {
            result.completeExceptionally(e);
        }
        debug.info("Solved continuation");
    }

    // It can happen that fixedpoint is called in the context of a running fixedpoint.
    // This can happen when a continuation is not triggered by a remote message, but
    // directly completed (e.g., by a try). The solveK invocation will call fixedpoint
    // again. To ensure we do not complete too early, it is necessary to track the number
    // of unsolved constraints in the current execution state (because of the direct
    // recursion of k), and only complete when there are no left. This is what the
    // ehpemeralActiveConstraints counter does.
    private void fixedpoint() throws InterruptedException {
        debug.info("Solving constraints");

        IConstraint constraint;
        while((constraint = constraints.remove()) != null) {
            ephemeralActiveConstraints.incrementAndGet();
            k(constraint, MAX_DEPTH);
        }

        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Fixed point finished with remaining constraints");
            throw new IllegalStateException(
                    "Expected no remaining active constraints, but got " + constraints.activeSize());
        }

        debug.info("Has ephermeral: {}, pending: {}, done: {}", ephemeralActiveConstraints.get(),
                scopeGraph.hasPending(), result.isDone());
        if(ephemeralActiveConstraints.get() == 0 && !scopeGraph.hasPending() && !result.isDone()) {
            debug.info("Finished.");
            result.completeValue(finishSolve());
        } else {
            debug.info("Not finished.");
        }
    }

    private SolverResult finishSolve() {
        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.info("Solved constraints with {} failed and {} remaining constraint(s).", failed.size(),
                constraints.delayedSize());
        for(Entry<IConstraint, Delay> entry : delayed.entrySet()) {
            debug.info(" * {} on {}", entry.getKey().toString(state.unifier()::toString), entry.getValue());
        }

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(ImmutableMap.of());
        final java.util.Set<CriticalEdge> removedEdges = ImmutableSet.of();
        final ICompleteness.Immutable completeness = Completeness.Immutable.of(spec);
        final SolverResult result =
                SolverResult.of(state, failed, delayed, existentials, updatedVars, removedEdges, completeness);
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // success/failure signals
    ///////////////////////////////////////////////////////////////////////////

    private Unit success(IConstraint constraint, IState.Immutable newState, Collection<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints, Map<Delay, IConstraint> delayedConstraints,
            Map<ITermVar, ITermVar> existentials, int fuel) throws InterruptedException {
        state = newState;

        final IDebugContext subDebug = debug.subContext();
        if(this.existentials == null) {
            this.existentials = existentials;
        }
        final IUniDisunifier.Immutable unifier = state.unifier();

        // updates from unified variables
        if(!updatedVars.isEmpty()) {
            releaseDelayedActions(updatedVars);
            final ICompleteness.Transient _completeness = completeness.melt();
            _completeness.updateAll(updatedVars, unifier);
            this.completeness = _completeness.freeze();
            constraints.activateFromVars(updatedVars, debug);
            this.updatedVars.addAll(updatedVars);
        }

        // add new constraints
        if(!newConstraints.isEmpty()) {
            // no constraints::addAll, instead recurse immediately below
            final ICompleteness.Transient _completeness = completeness.melt();
            _completeness.addAll(newConstraints, unifier); // must come before ICompleteness::remove
            this.completeness = _completeness.freeze();
            if(subDebug.isEnabled(Level.Info) && !newConstraints.isEmpty()) {
                subDebug.info("Simplified to:");
                for(IConstraint newConstraint : newConstraints) {
                    subDebug.info(" * {}", Solver.toString(newConstraint, unifier));
                }
            }
            ephemeralActiveConstraints.addAndGet(newConstraints.size());
        }

        // add delayed constraints
        if(!delayedConstraints.isEmpty()) {
            delayedConstraints.forEach((d, c) -> constraints.delay(c, d));
            final ICompleteness.Transient _completeness = completeness.melt();
            _completeness.addAll(delayedConstraints.values(), unifier); // must come before ICompleteness::remove
            this.completeness = _completeness.freeze();
            if(subDebug.isEnabled(Level.Info) && !delayedConstraints.isEmpty()) {
                subDebug.info("Delayed:");
                for(IConstraint delayedConstraint : delayedConstraints.values()) {
                    subDebug.info(" * {}", Solver.toString(delayedConstraint, unifier));
                }
            }
        }

        removeCompleteness(constraint);
        ephemeralActiveConstraints.decrementAndGet();

        // continue on new constraints
        for(IConstraint newConstraint : newConstraints) {
            k(newConstraint, fuel - 1);
        }

        return Unit.unit;
    }

    private Unit success(IConstraint c, IState.Immutable newState, int fuel) throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), ImmutableList.of(), ImmutableMap.of(), ImmutableMap.of(), fuel);
    }

    private Unit successNew(IConstraint c, IState.Immutable newState, Collection<IConstraint> newConstraints, int fuel)
            throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), newConstraints, ImmutableMap.of(), ImmutableMap.of(), fuel);
    }

    private Unit delay(IConstraint c, IState.Immutable newState, Delay delay, int fuel) throws InterruptedException {
        if(!delay.criticalEdges().isEmpty()) {
            debug.error("FIXME: query {} failed on critical edges {}", c.toString(state.unifier()::toString),
                    delay.criticalEdges());
            return fail(c);
        } else {
            final Set.Immutable<ITermVar> vars = delay.vars().stream().flatMap(v -> state.unifier().getVars(v).stream())
                    .collect(CapsuleCollectors.toSet());
            final Set.Immutable<ITermVar> foreignVars = Set.Immutable.subtract(vars, state.vars());
            if(!foreignVars.isEmpty()) {
                debug.error("FIXME: query {} failed on foreign vars {}", c.toString(state.unifier()::toString),
                        foreignVars);
                return fail(c);
            } else if(vars.isEmpty()) {
                debug.info("query {} delayed on no vars, rescheduling", c.toString(state.unifier()::toString));
                return success(c, newState, ImmutableSet.of(), ImmutableList.of(c), ImmutableMap.of(delay, c),
                        ImmutableMap.of(), fuel);
            } else {
                debug.info("query {} delayed on vars {}", c.toString(state.unifier()::toString), vars);
                return success(c, newState, ImmutableSet.of(), ImmutableList.of(), ImmutableMap.of(delay, c),
                        ImmutableMap.of(), fuel);
            }
        }
    }

    private <R> Unit future(IConstraint c, IState.Immutable newState, IFuture<R> future, K<R> k, int fuel)
            throws InterruptedException {
        future.handle((r, ex) -> {
            solveK(k, r, ex);
            return Unit.unit;
        });
        return Unit.unit;
    }

    private Unit fail(IConstraint constraint) throws InterruptedException {
        failed.put(constraint, MessageUtil.findClosestMessage(constraint));
        removeCompleteness(constraint);
        ephemeralActiveConstraints.decrementAndGet();
        return Unit.unit;
    }

    private void removeCompleteness(IConstraint constraint) throws InterruptedException {
        final ICompleteness.Transient _completeness = completeness.melt();
        final java.util.Set<CriticalEdge> removedEdges = _completeness.remove(constraint, state.unifier());
        for(CriticalEdge criticalEdge : removedEdges) {
            closeEdge(criticalEdge);
        }
        this.completeness = _completeness.freeze();
    }

    private Unit queue(IConstraint constraint) {
        ephemeralActiveConstraints.decrementAndGet();
        constraints.add(constraint);
        return Unit.unit;
    }

    ///////////////////////////////////////////////////////////////////////////
    // k
    ///////////////////////////////////////////////////////////////////////////

    private Unit k(IConstraint constraint, int fuel) throws InterruptedException {
        // stop if thread is interrupted
        if(cancel.cancelled() || Thread.interrupted()) {
            throw new InterruptedException();
        }

        // stop recursion if we run out of fuel
        if(fuel <= 0) {
            return queue(constraint);
        }

        if(debug.isEnabled(Level.Info)) {
            debug.info("Solving {}", constraint.toString(Solver.shallowTermFormatter(state.unifier())));
        }

        // solve
        return constraint.matchOrThrow(new IConstraint.CheckedCases<Unit, InterruptedException>() {

            @Override public Unit caseArith(CArith c) throws InterruptedException {
                final IUniDisunifier unifier = state.unifier();
                final Optional<ITerm> term1 = c.expr1().isTerm();
                final Optional<ITerm> term2 = c.expr2().isTerm();
                try {
                    if(c.op().isEquals() && term1.isPresent()) {
                        int i2 = c.expr2().eval(unifier);
                        final IConstraint eq = new CEqual(term1.get(), B.newInt(i2), c);
                        return successNew(c, state, ImmutableList.of(eq), fuel);
                    } else if(c.op().isEquals() && term2.isPresent()) {
                        int i1 = c.expr1().eval(unifier);
                        final IConstraint eq = new CEqual(B.newInt(i1), term2.get(), c);
                        return successNew(c, state, ImmutableList.of(eq), fuel);
                    } else {
                        int i1 = c.expr1().eval(unifier);
                        int i2 = c.expr2().eval(unifier);
                        if(c.op().test(i1, i2)) {
                            return success(c, state, fuel);
                        } else {
                            return fail(c);
                        }
                    }
                } catch(Delay d) {
                    return delay(c, state, d, fuel);
                }
            }

            @Override public Unit caseConj(CConj c) throws InterruptedException {
                final List<IConstraint> newConstraints = disjoin(c);
                return successNew(c, state, newConstraints, fuel);
            }

            @Override public Unit caseEqual(CEqual c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IUniDisunifier.Immutable unifier = state.unifier();
                try {
                    final IUniDisunifier.Result<IUnifier.Immutable> result;
                    if((result = unifier.unify(term1, term2).orElse(null)) != null) {
                        if(debug.isEnabled(Level.Info)) {
                            debug.info("Unification succeeded: {}", result.result());
                        }
                        final IState.Immutable newState = state.withUnifier(result.unifier());
                        final Set<ITermVar> updatedVars = result.result().varSet();
                        return success(c, newState, updatedVars, ImmutableList.of(), ImmutableMap.of(),
                                ImmutableMap.of(), fuel);
                    } else {
                        if(debug.isEnabled(Level.Info)) {
                            debug.info("Unification failed: {} != {}", unifier.toString(term1),
                                    unifier.toString(term2));
                        }
                        return fail(c);
                    }
                } catch(OccursException e) {
                    if(debug.isEnabled(Level.Info)) {
                        debug.info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
                    }
                    return fail(c);
                }
            }

            @Override public Unit caseExists(CExists c) throws InterruptedException {
                final ImmutableMap.Builder<ITermVar, ITermVar> existentialsBuilder = ImmutableMap.builder();
                IState.Immutable newState = state;
                for(ITermVar var : c.vars()) {
                    final Tuple2<ITermVar, IState.Immutable> varAndState = newState.freshVar(var);
                    final ITermVar freshVar = varAndState._1();
                    newState = varAndState._2();
                    existentialsBuilder.put(var, freshVar);
                }
                final Map<ITermVar, ITermVar> existentials = existentialsBuilder.build();
                final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(existentials);
                final IConstraint newConstraint = c.constraint().apply(subst).withCause(c.cause().orElse(null));
                return success(c, newState, ImmutableSet.of(), disjoin(newConstraint), ImmutableMap.of(), existentials,
                        fuel);
            }

            @Override public Unit caseFalse(CFalse c) throws InterruptedException {
                return fail(c);
            }

            @Override public Unit caseInequal(CInequal c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                final IUniDisunifier.Immutable unifier = state.unifier();
                final IUniDisunifier.Result<Optional<Diseq>> result;
                if((result = unifier.disunify(c.universals(), term1, term2).orElse(null)) != null) {
                    if(debug.isEnabled(Level.Info)) {
                        debug.info("Disunification succeeded: {}", result);
                    }
                    final IState.Immutable newState = state.withUnifier(result.unifier());
                    final Set<ITermVar> updatedVars =
                            result.result().<Set<ITermVar>>map(Diseq::varSet).orElse(Set.Immutable.of());
                    return success(c, newState, updatedVars, ImmutableList.of(), ImmutableMap.of(), ImmutableMap.of(),
                            fuel);
                } else {
                    if(debug.isEnabled(Level.Info)) {
                        debug.info("Disunification failed");
                    }
                    return fail(c);
                }
            }

            @Override public Unit caseNew(CNew c) throws InterruptedException {
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm datumTerm = c.datumTerm();
                final String name = M.var(ITermVar::getName).match(scopeTerm).orElse("s");
                final List<ITerm> labels = getOpenEdges(scopeTerm);

                final IFuture<Scope> futureScope =
                        scopeGraph.freshScope(name, labels, ImmutableList.of(Access.INTERNAL, Access.EXTERNAL));
                final K<Scope> k = (scope, ex, fuel) -> {
                    if(ex != null) {
                        return fail(c);
                    }
                    scopeGraph.setDatum(scope, datumTerm, Access.INTERNAL);
                    delayAction(() -> {
                        scopeGraph.setDatum(scope, state.unifier().findRecursive(datumTerm), Access.EXTERNAL);
                    }, state.unifier().getVars(datumTerm));
                    final IConstraint eq = new CEqual(scopeTerm, scope, c);
                    return successNew(c, state, ImmutableList.of(eq), fuel);
                };
                return future(c, state, futureScope, k, fuel);
            }

            @Override public Unit caseResolveQuery(CResolveQuery c) throws InterruptedException {
                final ITerm scopeTerm = c.scopeTerm();
                final IQueryFilter filter = c.filter();
                final IQueryMin min = c.min();
                final ITerm resultTerm = c.resultTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(scopeTerm)), fuel);
                }
                final Scope scope = AScope.matcher().match(scopeTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

                final ConstraintContext params = new ConcurrentConstraintContext(debug);
                final ConstraintQueries cq = new ConstraintQueries(spec, state, params, progress, cancel);
                final LabelWF<ITerm> labelWF = cq.getLabelWF(filter.getLabelWF());
                final DataWF<ITerm> dataWF = cq.getDataWF(filter.getDataWF());
                final LabelOrder<ITerm> labelOrder = cq.getLabelOrder(min.getLabelOrder());
                final DataLeq<ITerm> dataEquiv = cq.getDataEquiv(min.getDataEquiv());

                final IFuture<java.util.Set<IResolutionPath<Scope, ITerm, ITerm>>> future =
                        scopeGraph.query(scope, labelWF, dataWF, labelOrder, dataEquiv);
                final K<java.util.Set<IResolutionPath<Scope, ITerm, ITerm>>> k = (paths, ex, fuel) -> {
                    if(ex != null) {
                        // pattern matching for the brave and stupid
                        try {
                            throw ex;
                        } catch(ResolutionDelayException rde) {
                            final Delay delay = rde.getCause();
                            return delay(c, state, delay, fuel);
                        } catch(DeadLockedException dle) {
                            debug.error("query {} deadlocked", c.toString(state.unifier()::toString));
                            return fail(c);
                        } catch(Throwable t) {
                            debug.error("query {} failed", t, c.toString(state.unifier()::toString));
                            return fail(c);
                        }
                    } else {
                        final List<ITerm> pathTerms =
                                paths.stream().map(p -> StatixTerms.explicate(p, spec.dataLabels()))
                                        .collect(ImmutableList.toImmutableList());
                        final IConstraint C = new CEqual(resultTerm, B.newList(pathTerms), c);
                        return successNew(c, state, ImmutableList.of(C), fuel);
                    }
                };
                return future(c, state, future, k, fuel);
            }

            @Override public Unit caseTellEdge(CTellEdge c) throws InterruptedException {
                final ITerm sourceTerm = c.sourceTerm();
                final ITerm label = c.label();
                final ITerm targetTerm = c.targetTerm();
                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(sourceTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(sourceTerm)), fuel);
                }
                if(!unifier.isGround(targetTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(targetTerm)), fuel);
                }
                final Scope source =
                        AScope.matcher().match(sourceTerm, unifier).orElseThrow(() -> new IllegalArgumentException(
                                "Expected source scope, got " + unifier.toString(sourceTerm)));
                final Scope target =
                        AScope.matcher().match(targetTerm, unifier).orElseThrow(() -> new IllegalArgumentException(
                                "Expected target scope, got " + unifier.toString(targetTerm)));
                scopeGraph.addEdge(source, label, target);
                return success(c, state, fuel);
            }

            @Override public Unit caseTermId(CAstId c) throws InterruptedException {
                final ITerm term = c.astTerm();
                final ITerm idTerm = c.idTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!(unifier.isGround(term))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(term)), fuel);
                }
                final CEqual eq;
                final Optional<Scope> maybeScope = AScope.matcher().match(term, unifier);
                if(maybeScope.isPresent()) {
                    final AScope scope = maybeScope.get();
                    eq = new CEqual(idTerm, scope);
                    return successNew(c, state, ImmutableList.of(eq), fuel);
                } else {
                    final Optional<TermIndex> maybeIndex = TermIndex.get(unifier.findTerm(term));
                    if(maybeIndex.isPresent()) {
                        final ITerm indexTerm = TermOrigin.copy(term, maybeIndex.get());
                        eq = new CEqual(idTerm, indexTerm);
                        return successNew(c, state, ImmutableList.of(eq), fuel);
                    } else {
                        return fail(c);
                    }
                }
            }

            @Override public Unit caseTermProperty(CAstProperty c) throws InterruptedException {
                final ITerm idTerm = c.idTerm();
                final ITerm prop = c.property();
                final ITerm value = c.value();

                final IUniDisunifier unifier = state.unifier();
                if(!(unifier.isGround(idTerm))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(idTerm)), fuel);
                }
                final Optional<TermIndex> maybeIndex = TermIndex.matcher().match(idTerm, unifier);
                if(maybeIndex.isPresent()) {
                    final TermIndex index = maybeIndex.get();
                    final Tuple2<TermIndex, ITerm> key = Tuple2.of(index, prop);
                    ITermProperty property;
                    switch(c.op()) {
                        case ADD: {
                            property = state.termProperties().getOrDefault(key, BagTermProperty.of());
                            if(!property.multiplicity().equals(Multiplicity.BAG)) {
                                return fail(c);
                            }
                            property = property.addValue(value);
                            break;
                        }
                        case SET: {
                            if(state.termProperties().containsKey(key)) {
                                return fail(c);
                            }
                            property = SingletonTermProperty.of(value);
                            break;
                        }
                        default:
                            throw new IllegalStateException("Unknown op " + c.op());
                    }
                    final IState.Immutable newState =
                            state.withTermProperties(state.termProperties().__put(key, property));
                    return success(c, newState, fuel);
                } else {
                    return fail(c);
                }
            }

            @Override public Unit caseTrue(CTrue c) throws InterruptedException {
                return success(c, state, fuel);
            }

            @Override public Unit caseTry(CTry c) throws InterruptedException {
                final IDebugContext subDebug = debug.subContext();
                final IState.Immutable subState = state.withResource(state.resource() + "#try");
                final StatixSolver subSolver = new StatixSolver(c.constraint(), spec, subState, completeness, subDebug,
                        progress, cancel, scopeGraph.getSubScopeGraph());
                final IFuture<SolverResult> subResult = subSolver.entail();
                final K<SolverResult> k = (r, ex, fuel) -> {
                    if(ex != null) {
                        debug.error("try {} failed", ex, c.toString(state.unifier()::toString));
                        return fail(c);
                    } else {
                        try {
                            // check entailment w.r.t. the initial substate, not the current state: otherwise,
                            // some variables may be treated as external while they are not
                            if(Solver.entails(subState, r, subDebug)) {
                                debug.info("constraint {} entailed", c.toString(state.unifier()::toString));
                                return success(c, state, fuel);
                            } else {
                                debug.info("constraint {} not entailed", c.toString(state.unifier()::toString));
                                return fail(c);

                            }
                        } catch(Delay delay) {
                            return delay(c, state, delay, fuel);
                        }
                    }
                };
                return future(c, state, subResult, k, fuel);
            }

            @Override public Unit caseUser(CUser c) throws InterruptedException {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final LazyDebugContext proxyDebug = new LazyDebugContext(debug);

                final List<Rule> rules = spec.rules().getRules(name);
                final List<Tuple2<Rule, ApplyResult>> results = RuleUtil.applyOrderedAll(state, rules, args, c);
                if(results.isEmpty()) {
                    debug.info("No rule applies");
                    return fail(c);
                } else if(results.size() == 1) {
                    final ApplyResult applyResult = results.get(0)._2();
                    proxyDebug.info("Rule accepted");
                    proxyDebug.info("| Implied equalities: {}", applyResult.diff());
                    proxyDebug.commit();
                    return success(c, applyResult.state(), applyResult.diff().varSet(), disjoin(applyResult.body()),
                            ImmutableMap.of(), ImmutableMap.of(), fuel);
                } else {
                    final Set<ITermVar> stuckVars = results.stream().flatMap(r -> Streams.stream(r._2().guard()))
                            .flatMap(g -> g.varSet().stream()).collect(CapsuleCollectors.toSet());
                    proxyDebug.info("Rule delayed (multiple conditional matches)");
                    return delay(c, state, Delay.ofVars(stuckVars), fuel);
                }
            }

        });

    }

    ///////////////////////////////////////////////////////////////////////////
    // Open edges & delayed closes
    ///////////////////////////////////////////////////////////////////////////

    private Set.Transient<CriticalEdge> delayedCloses = Set.Transient.of();

    private List<ITerm> getOpenEdges(ITerm varOrScope) {
        // we must include queued edge closes here, to ensure we registered the open
        // edge when the close is released
        final Stream<EdgeOrData<ITerm>> openEdges = Streams.stream(completeness.get(varOrScope, state.unifier()));
        final Stream<EdgeOrData<ITerm>> queuedEdges = M
                .var().match(varOrScope).map(var -> delayedCloses.stream()
                        .filter(e -> state.unifier().findRecursive(var).equals(varOrScope)).map(e -> e.edgeOrData()))
                .orElse(Stream.<EdgeOrData<ITerm>>empty());
        return Streams.concat(openEdges, queuedEdges).<ITerm>flatMap(eod -> {
            return eod.match(acc -> Stream.<ITerm>empty(), (l) -> Stream.of(l));
        }).collect(Collectors.toList());
    }

    private void closeEdge(CriticalEdge criticalEdge) throws InterruptedException {
        debug.info("client {} close edge {}/{}", this, state.unifier().toString(criticalEdge.scope()),
                criticalEdge.edgeOrData());
        delayedCloses.__insert(criticalEdge);
        delayAction(() -> {
            delayedCloses.__remove(criticalEdge);
            closeGroundEdge(criticalEdge);
        }, state.unifier().getVars(criticalEdge.scope()));
    }

    private void closeGroundEdge(CriticalEdge criticalEdge) {
        debug.info("client {} close edge {}/{}", this, state.unifier().toString(criticalEdge.scope()),
                criticalEdge.edgeOrData());
        final Scope scope = Scope.matcher().match(criticalEdge.scope(), state.unifier())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Expected scope, got " + state.unifier().toString(criticalEdge.scope())));
        // @formatter:off
        criticalEdge.edgeOrData().match(
            acc -> {
                // ignore data labels, they are managed separately
                return Unit.unit;
            },
            label -> {
                scopeGraph.closeEdge(scope, label);
                return Unit.unit;
            }
        );
        // @formatter:on
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delayed actions
    ///////////////////////////////////////////////////////////////////////////

    private final VarIndexedCollection<CheckedAction0<InterruptedException>> delayedActions =
            new VarIndexedCollection<>();

    private void delayAction(CheckedAction0<InterruptedException> action, Iterable<ITermVar> vars)
            throws InterruptedException {
        if(!delayedActions.put(action, vars, state.unifier())) {
            action.apply();
        }
    }

    private void releaseDelayedActions(Iterable<ITermVar> updatedVars) throws InterruptedException {
        for(CheckedAction0<InterruptedException> action : delayedActions.update(updatedVars, state.unifier())) {
            action.apply();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "StatixSolver";
    }

    ///////////////////////////////////////////////////////////////////////////
    // K
    ///////////////////////////////////////////////////////////////////////////

    @FunctionalInterface
    private interface K<R> {

        Unit k(R result, Throwable ex, int fuel) throws InterruptedException;

    }

}