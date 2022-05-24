package mb.statix.concurrent;

import static com.google.common.collect.Streams.stream;
import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.nabl2.terms.matching.Transform.T;
import static mb.statix.constraints.Constraints.disjoin;
import static mb.statix.solver.persistent.Solver.INCREMENTAL_CRITICAL_EDGES;
import static mb.statix.solver.persistent.Solver.RETURN_ON_FIRST_ERROR;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.CheckedAction0;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullProgress;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.Renaming;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.p_raffrayi.DeadlockException;
import mb.p_raffrayi.ITypeCheckerContext;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.ecoop21.RegExpLabelWf;
import mb.scopegraph.ecoop21.RelationLabelOrder;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.patching.IPatchCollection;
import mb.statix.concurrent.util.Patching;
import mb.statix.concurrent.util.VarIndexedCollection;
import mb.statix.constraints.CArith;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CCompiledQuery;
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
import mb.statix.constraints.Constraints;
import mb.statix.constraints.IResolveQuery;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.scopegraph.AScope;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.IState;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.ITermProperty.Multiplicity;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.CompletenessUtil;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.BagTermProperty;
import mb.statix.solver.persistent.SingletonTermProperty;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.Solver.PreSolveResult;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.solver.store.BaseConstraintStore;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyMode.Safety;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

public class StatixSolver {

    private enum ShadowOptimization {
        NONE, RULE, CONTEXT
    }

    private static final ShadowOptimization SHADOW_OPTIMIZATION = ShadowOptimization.RULE;

    private static final boolean LOCAL_INFERENCE = true;

    private static final ImmutableSet<ITermVar> NO_UPDATED_VARS = ImmutableSet.of();
    private static final ImmutableList<IConstraint> NO_NEW_CONSTRAINTS = ImmutableList.of();
    private static final mb.statix.solver.completeness.Completeness.Immutable NO_NEW_CRITICAL_EDGES =
            Completeness.Immutable.of();
    private static final ImmutableMap<ITermVar, ITermVar> NO_EXISTENTIALS = ImmutableMap.of();

    private static final int MAX_DEPTH = 32;

    private final Spec spec;
    private final IConstraintStore constraints;
    private final IDebugContext debug;
    private final IProgress progress;
    private final ICancel cancel;
    private final ITypeCheckerContext<Scope, ITerm, ITerm> scopeGraph;
    private final int flags;

    private IState.Immutable state;
    private ICompleteness.Immutable completeness;
    private Map<ITermVar, ITermVar> existentials = null;
    private final List<ITermVar> updatedVars = Lists.newArrayList();
    private final Map<IConstraint, IMessage> failed = Maps.newHashMap();

    private final AtomicBoolean inFixedPoint = new AtomicBoolean(false);
    private final Set.Transient<IConstraint> pendingConstraints = CapsuleUtil.transientSet();
    private final CompletableFuture<SolverResult> result;

    public StatixSolver(IConstraint constraint, Spec spec, IState.Immutable state, ICompleteness.Immutable completeness,
            IDebugContext debug, IProgress progress, ICancel cancel,
            ITypeCheckerContext<Scope, ITerm, ITerm> scopeGraph, int flags) {
        if(INCREMENTAL_CRITICAL_EDGES && !spec.hasPrecomputedCriticalEdges()) {
            debug.warn("Leaving precomputing critical edges to solver may result in duplicate work.");
            this.spec = spec.precomputeCriticalEdges();
        } else {
            this.spec = spec;
        }
        this.scopeGraph = scopeGraph;
        this.state = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        final ICompleteness.Transient _completeness = completeness.melt();
        if(INCREMENTAL_CRITICAL_EDGES) {
            final Tuple2<IConstraint, ICompleteness.Immutable> initialConstraintAndCriticalEdges =
                    CompletenessUtil.precomputeCriticalEdges(constraint, spec.scopeExtensions());
            this.constraints.add(initialConstraintAndCriticalEdges._1());
            _completeness.addAll(initialConstraintAndCriticalEdges._2(), state.unifier());
        } else {
            constraints.add(constraint);
            _completeness.add(constraint, spec, state.unifier());
        }
        this.completeness = _completeness.freeze();
        this.result = new CompletableFuture<>();
        this.progress = progress;
        this.cancel = cancel;
        this.flags = flags;
    }

    public StatixSolver(SolverState state, Spec spec, IDebugContext debug, IProgress progress, ICancel cancel,
            ITypeCheckerContext<Scope, ITerm, ITerm> scopeGraph, int flags) {
        if(INCREMENTAL_CRITICAL_EDGES && !spec.hasPrecomputedCriticalEdges()) {
            debug.warn("Leaving precomputing critical edges to solver may result in duplicate work.");
            this.spec = spec.precomputeCriticalEdges();
        } else {
            this.spec = spec;
        }
        this.scopeGraph = scopeGraph;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        this.result = new CompletableFuture<>();
        this.progress = progress;
        this.cancel = cancel;
        this.flags = flags;

        this.state = state.state();
        this.completeness = state.completeness();
        this.constraints.addAll(state.constraints());
        this.existentials = state.existentials();
        this.updatedVars.addAll(state.updatedVars());
        this.failed.putAll(state.failed());
        try {
            for(CriticalEdge criticalEdge : state.delayedCloses()) {
                closeEdge(criticalEdge);
            }
        } catch(InterruptedException e) {
            result.completeExceptionally(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // driver
    ///////////////////////////////////////////////////////////////////////////

    public IFuture<SolverResult> solve(Iterable<Scope> roots) {
        try {
            for(Scope root : CapsuleUtil.toSet(roots)) {
                final Set.Immutable<ITerm> openEdges = getOpenEdges(root);
                scopeGraph.initScope(root, openEdges, false);
            }
            fixedpoint();
        } catch(Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    public IFuture<SolverResult> continueSolve() {
        try {
            fixedpoint();
        } catch(Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    public IFuture<SolverResult> entail() {
        return solve(Collections.emptyList());
    }

    private <R> void solveK(K<R> k, R r, Throwable ex) {
        debug.debug("Solving continuation");
        try {
            if(!k.k(r, ex, MAX_DEPTH)) {
                debug.debug("Finished fast.");
                result.complete(finishSolve());
                return;
            }
            fixedpoint();
        } catch(Throwable e) {
            result.completeExceptionally(e);
        }
        debug.debug("Solved continuation");
    }


    // It can happen that fixedpoint is called in the context of a running fixedpoint.
    // This can happen when a continuation is not triggered by a remote message, but
    // directly completed (e.g., by a try). The solveK invocation will call fixedpoint
    // again. We prevent recursive fixed points to ensure the termination conditions are
    // correctly checked.
    private void fixedpoint() throws InterruptedException {
        if(!inFixedPoint.compareAndSet(false, true)) {
            return;
        }

        debug.debug("Solving constraints");

        IConstraint constraint;
        while((constraint = constraints.remove()) != null) {
            if(!k(constraint, MAX_DEPTH)) {
                debug.debug("Finished fast.");
                result.complete(finishSolve());
                return;
            }
        }

        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Fixed point finished with remaining constraints");
            throw new IllegalStateException(
                    "Expected no remaining active constraints, but got " + constraints.activeSize());
        }

        debug.debug("Has pending: {}, done: {}", pendingConstraints.size(), result.isDone());
        if(pendingConstraints.size() == 0 && !result.isDone()) {
            debug.debug("Finished.");
            result.complete(finishSolve());
        } else {
            debug.debug("Not finished.");
        }

        if(!inFixedPoint.compareAndSet(true, false)) {
            throw new IllegalStateException("Fixed point nesting detection error.");
        }
    }

    private SolverResult finishSolve() throws InterruptedException {
        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.debug("Solved constraints with {} failed and {} remaining constraint(s).", failed.size(),
                constraints.delayedSize());
        if(debug.isEnabled(Level.Debug)) {
            for(Map.Entry<IConstraint, Delay> entry : delayed.entrySet()) {
                debug.debug(" * {} on {}", entry.getKey().toString(state.unifier()::toString), entry.getValue());
            }
        }

        // cleanup open edges
        for(IConstraint delay : delayed.keySet()) {
            removeCompleteness(delay);
        }

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(NO_EXISTENTIALS);
        final java.util.Set<CriticalEdge> removedEdges = ImmutableSet.of();
        final ICompleteness.Immutable completeness = Completeness.Immutable.of();
        final SolverResult result =
                SolverResult.of(spec, state, failed, delayed, existentials, updatedVars, removedEdges, completeness);
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // success/failure signals
    ///////////////////////////////////////////////////////////////////////////

    private boolean success(IConstraint constraint, IState.Immutable newState, Collection<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints, ICompleteness.Immutable newCriticalEdges,
            Map<ITermVar, ITermVar> existentials, int fuel) throws InterruptedException {
        state = newState;

        final IDebugContext subDebug = debug.subContext();
        if(this.existentials == null) {
            this.existentials = existentials;
        }
        final IUniDisunifier.Immutable unifier = state.unifier();

        // updates from unified variables
        if(!updatedVars.isEmpty()) {
            final ICompleteness.Transient _completeness = completeness.melt();
            _completeness.updateAll(updatedVars, unifier);
            this.completeness = _completeness.freeze();
            constraints.activateFromVars(updatedVars, debug);
            this.updatedVars.addAll(updatedVars);
        }

        // add new constraints
        if(!newConstraints.isEmpty()) {
            // no constraints::addAll, instead recurse in tail position
            final ICompleteness.Transient _completeness = completeness.melt();
            if(INCREMENTAL_CRITICAL_EDGES) {
                _completeness.addAll(newCriticalEdges, unifier); // must come before ICompleteness::remove
            } else {
                _completeness.addAll(newConstraints, spec, unifier); // must come before ICompleteness::remove
            }
            this.completeness = _completeness.freeze();
            if(subDebug.isEnabled(Level.Debug) && !newConstraints.isEmpty()) {
                subDebug.debug("Simplified to:");
                for(IConstraint newConstraint : newConstraints) {
                    subDebug.debug(" * {}", Solver.toString(newConstraint, unifier));
                }
            }
        }

        removeCompleteness(constraint);

        // do this after the state has been completely updated
        if(!updatedVars.isEmpty()) {
            releaseDelayedActions(updatedVars);
        }

        // continue on new constraints
        for(IConstraint newConstraint : newConstraints) {
            if(!k(newConstraint, fuel - 1)) {
                return false;
            }
        }

        return true;
    }

    private boolean delay(IConstraint constraint, Delay delay) throws InterruptedException {
        if(!delay.criticalEdges().isEmpty()) {
            debug.error("FIXME: constraint failed on critical edges {}: {}", delay.criticalEdges(),
                    constraint.toString(state.unifier()::toString));
            return fail(constraint);
        }

        final Set.Immutable<ITermVar> vars = delay.vars().stream().flatMap(v -> state.unifier().getVars(v).stream())
                .collect(CapsuleCollectors.toSet());
        if(vars.isEmpty()) {
            debug.error("FIXME: constraint delayed on no vars: {}", delay.criticalEdges(),
                    constraint.toString(state.unifier()::toString));
            return fail(constraint);
        }

        if(debug.isEnabled(Level.Debug)) {
            debug.debug("constraint delayed on vars {}: {}", vars, constraint.toString(state.unifier()::toString));
        }

        final IDebugContext subDebug = debug.subContext();
        constraints.delay(constraint, delay);
        if(subDebug.isEnabled(Level.Debug)) {
            subDebug.debug("Delayed: {}", Solver.toString(constraint, state.unifier()));
        }

        return true;
    }

    private <R> boolean future(IConstraint constraint, IFuture<R> future, K<? super R> k) throws InterruptedException {
        pendingConstraints.__insert(constraint);
        future.handle((r, ex) -> {
            pendingConstraints.__remove(constraint);
            if(!result.isDone()) {
                solveK(k, r, ex);
            }
            return Unit.unit;
        });
        return true;
    }

    private boolean fail(IConstraint constraint) throws InterruptedException {
        final IMessage message = MessageUtil.findClosestMessage(constraint);
        failed.put(constraint, message);
        removeCompleteness(constraint);
        return message.kind() != MessageKind.ERROR || (flags & RETURN_ON_FIRST_ERROR) == 0;
    }

    private void removeCompleteness(IConstraint constraint) throws InterruptedException {
        final Set.Immutable<CriticalEdge> removedEdges;
        final ICompleteness.Transient _completeness = completeness.melt();
        if(INCREMENTAL_CRITICAL_EDGES) {
            if(!constraint.ownCriticalEdges().isPresent()) {
                throw new IllegalArgumentException("Solver only accepts constraints with pre-computed critical edges.");
            }
            removedEdges = _completeness.removeAll(constraint.ownCriticalEdges().get(), state.unifier());
        } else {
            removedEdges = _completeness.remove(constraint, spec, state.unifier());
        }
        for(CriticalEdge criticalEdge : removedEdges) {
            closeEdge(criticalEdge);
        }
        this.completeness = _completeness.freeze();
    }

    private boolean queue(IConstraint constraint) {
        constraints.add(constraint);
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // k
    ///////////////////////////////////////////////////////////////////////////

    private boolean k(IConstraint constraint, int fuel) throws InterruptedException {
        // stop if thread is interrupted
        if(cancel.cancelled()) {
            throw new InterruptedException();
        }

        // stop recursion if we run out of fuel
        if(fuel <= 0) {
            return queue(constraint);
        }

        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Solving {}",
                    constraint.toString(Solver.shallowTermFormatter(state.unifier(), Solver.TERM_FORMAT_DEPTH)));
        }

        // solve
        return constraint.matchOrThrow(new IConstraint.CheckedCases<Boolean, InterruptedException>() {

            @Override public Boolean caseArith(CArith c) throws InterruptedException {
                final IUniDisunifier unifier = state.unifier();
                final Optional<ITerm> term1 = c.expr1().isTerm();
                final Optional<ITerm> term2 = c.expr2().isTerm();
                try {
                    if(c.op().isEquals() && term1.isPresent()) {
                        int i2 = c.expr2().eval(unifier);
                        final IConstraint eq = new CEqual(term1.get(), B.newInt(i2), c);
                        return success(c, state, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else if(c.op().isEquals() && term2.isPresent()) {
                        int i1 = c.expr1().eval(unifier);
                        final IConstraint eq = new CEqual(B.newInt(i1), term2.get(), c);
                        return success(c, state, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        int i1 = c.expr1().eval(unifier);
                        int i2 = c.expr2().eval(unifier);
                        if(c.op().test(i1, i2)) {
                            return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                    NO_EXISTENTIALS, fuel);
                        } else {
                            return fail(c);
                        }
                    }
                } catch(Delay d) {
                    return delay(c, d);
                }
            }

            @Override public Boolean caseConj(CConj c) throws InterruptedException {
                return success(c, state, NO_UPDATED_VARS, disjoin(c), NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS, fuel);
            }

            @Override public Boolean caseEqual(CEqual c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IUniDisunifier.Immutable unifier = state.unifier();
                try {
                    final IUniDisunifier.Result<IUnifier.Immutable> result;
                    if((result = unifier.unify(term1, term2, v -> isRigid(v, state)).orElse(null)) != null) {
                        if(debug.isEnabled(Level.Debug)) {
                            debug.debug("Unification succeeded: {}", result.result());
                        }
                        final IState.Immutable newState = state.withUnifier(result.unifier());
                        final Set<ITermVar> updatedVars = result.result().domainSet();
                        return success(c, newState, updatedVars, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        if(debug.isEnabled(Level.Debug)) {
                            debug.debug("Unification failed: {} != {}", unifier.toString(term1),
                                    unifier.toString(term2));
                        }
                        return fail(c);
                    }
                } catch(OccursException e) {
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
                    }
                    return fail(c);
                } catch(RigidException e) {
                    return delay(c, Delay.ofVars(e.vars()));
                }
            }

            @Override public Boolean caseExists(CExists c) throws InterruptedException {
                final Renaming.Builder _existentials = Renaming.builder();
                IState.Immutable newState = state;
                for(ITermVar var : c.vars()) {
                    final Tuple2<ITermVar, IState.Immutable> varAndState = newState.freshVar(var);
                    final ITermVar freshVar = varAndState._1();
                    newState = varAndState._2();
                    _existentials.put(var, freshVar);
                }
                final Renaming existentials = _existentials.build();

                final ISubstitution.Immutable subst = existentials.asSubstitution();
                final IConstraint newConstraint = c.constraint().apply(subst).withCause(c.cause().orElse(null));
                if(INCREMENTAL_CRITICAL_EDGES && !c.bodyCriticalEdges().isPresent()) {
                    throw new IllegalArgumentException(
                            "Solver only accepts constraints with pre-computed critical edges.");
                }
                final ICompleteness.Immutable newCriticalEdges =
                        c.bodyCriticalEdges().orElse(NO_NEW_CRITICAL_EDGES).apply(subst);
                return success(c, newState, NO_UPDATED_VARS, disjoin(newConstraint), newCriticalEdges,
                        existentials.asMap(), fuel);
            }

            @Override public Boolean caseFalse(CFalse c) throws InterruptedException {
                return fail(c);
            }

            @Override public Boolean caseInequal(CInequal c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                final IUniDisunifier.Immutable unifier = state.unifier();
                try {
                    final IUniDisunifier.Result<Optional<Diseq>> result;
                    if((result = unifier.disunify(c.universals(), term1, term2, v -> isRigid(v, state))
                            .orElse(null)) != null) {
                        if(debug.isEnabled(Level.Debug)) {
                            debug.debug("Disunification succeeded: {}", result);
                        }
                        final IState.Immutable newState = state.withUnifier(result.unifier());
                        final java.util.Set<ITermVar> updatedVars =
                                result.result().<java.util.Set<ITermVar>>map(Diseq::domainSet).orElse(NO_UPDATED_VARS);
                        return success(c, newState, updatedVars, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        if(debug.isEnabled(Level.Debug)) {
                            debug.debug("Disunification failed");
                        }
                        return fail(c);
                    }
                } catch(RigidException e) {
                    return delay(c, Delay.ofVars(e.vars()));
                }
            }

            @Override public Boolean caseNew(CNew c) throws InterruptedException {
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm datumTerm = c.datumTerm();
                final String name = M.var(ITermVar::getName).match(scopeTerm).orElse("s");
                final Set<ITerm> labels = getOpenEdges(scopeTerm);

                final Scope scope = scopeGraph.freshScope(name, labels, true, false);
                scopeGraph.setDatum(scope, datumTerm);
                final IConstraint eq = new CEqual(scopeTerm, scope, c);
                return success(c, state, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS,
                        fuel);
            }

            @Override public Boolean caseResolveQuery(IResolveQuery c) throws InterruptedException {
                final QueryFilter filter = c.filter();
                final QueryMin min = c.min();
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm resultTerm = c.resultTerm();

                final IUniDisunifier unifier = state.unifier();
                // @formatter:off
                final Set.Immutable<ITermVar> freeVars = Streams.concat(
                        unifier.getVars(scopeTerm).stream(),
                        filter.getDataWF().freeVars().stream().flatMap(v -> unifier.getVars(v).stream()),
                        min.getDataEquiv().freeVars().stream().flatMap(v -> unifier.getVars(v).stream())
                ).collect(CapsuleCollectors.toSet());
                // @formatter:on
                if(!freeVars.isEmpty()) {
                    return delay(c, Delay.ofVars(freeVars));
                }
                final Rule dataWfRule = RuleUtil.instantiateHeadPatterns(
                        RuleUtil.closeInUnifier(filter.getDataWF(), state.unifier(), Safety.UNSAFE));
                final Rule dataLeqRule = RuleUtil.instantiateHeadPatterns(
                        RuleUtil.closeInUnifier(min.getDataEquiv(), state.unifier(), Safety.UNSAFE));

                final Scope scope = AScope.matcher().match(scopeTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

                final LabelWf<ITerm> labelWF = new RegExpLabelWf<>(filter.getLabelWF());
                final DataWf<Scope, ITerm, ITerm> dataWF = new ConstraintDataWF(spec, dataWfRule);
                final DataLeq<Scope, ITerm, ITerm> dataEquiv = new ConstraintDataEquiv(spec, dataLeqRule);
                final DataWf<Scope, ITerm, ITerm> dataWFInternal =
                        LOCAL_INFERENCE ? new ConstraintDataWFInternal(dataWfRule) : null;
                final DataLeq<Scope, ITerm, ITerm> dataEquivInternal =
                        LOCAL_INFERENCE ? new ConstraintDataEquivInternal(dataLeqRule) : null;

                final IFuture<? extends java.util.Set<IResolutionPath<Scope, ITerm, ITerm>>> future;
                if((flags & Solver.FORCE_INTERP_QUERIES) == 0) {
                    // @formatter:off
                    future = c.match(new IResolveQuery.Cases<IFuture<? extends java.util.Set<IResolutionPath<Scope, ITerm, ITerm>>>>() {

                        @Override public IFuture<? extends java.util.Set<IResolutionPath<Scope, ITerm, ITerm>>> caseResolveQuery(CResolveQuery q) {
                            final LabelOrder<ITerm> labelOrder = new RelationLabelOrder<>(min.getLabelOrder());
                            return scopeGraph.query(scope, labelWF, labelOrder, dataWF, dataEquiv,
                                    dataWFInternal, dataEquivInternal);
                        }

                        @Override public IFuture<? extends java.util.Set<IResolutionPath<Scope, ITerm, ITerm>>> caseCompiledQuery(CCompiledQuery q) {
                            return scopeGraph.query(scope, q.stateMachine(), labelWF, dataWF, dataEquiv,
                                    dataWFInternal, dataEquivInternal);
                        }

                    });
                    // @formatter:on
                } else {
                    final LabelOrder<ITerm> labelOrder = new RelationLabelOrder<>(min.getLabelOrder());
                    future = scopeGraph.query(scope, labelWF, labelOrder, dataWF, dataEquiv, dataWFInternal,
                            dataEquivInternal);
                }

                final K<java.util.Set<IResolutionPath<Scope, ITerm, ITerm>>> k = (paths, ex, fuel) -> {
                    if(ex != null) {
                        // pattern matching for the brave and stupid
                        try {
                            throw ex;
                        } catch(ResolutionDelayException rde) {
                            if(debug.isEnabled(Level.Debug)) {
                                debug.debug("delayed query (unsupported) {}", rde,
                                        c.toString(state.unifier()::toString));
                            }
                            return fail(c);
                        } catch(DeadlockException dle) {
                            if(debug.isEnabled(Level.Debug)) {
                                debug.debug("deadlocked query (spec error) {}", c.toString(state.unifier()::toString));
                            }
                            return fail(c);
                        } catch(InterruptedException t) {
                            throw t;
                        } catch(Throwable t) {
                            debug.error("failed query {}", t, c.toString(state.unifier()::toString));
                            return fail(c);
                        }
                    } else {
                        final List<ITerm> pathTerms =
                                paths.stream().map(p -> StatixTerms.pathToTerm(p, spec.dataLabels()))
                                        .collect(ImmutableList.toImmutableList());
                        final IConstraint C = new CEqual(resultTerm, B.newList(pathTerms), c);
                        return success(c, state, NO_UPDATED_VARS, ImmutableList.of(C), NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    }
                };
                return future(c, future, k);
            }

            @Override public Boolean caseTellEdge(CTellEdge c) throws InterruptedException {
                final ITerm sourceTerm = c.sourceTerm();
                final ITerm label = c.label();
                final ITerm targetTerm = c.targetTerm();
                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(sourceTerm)) {
                    return delay(c, Delay.ofVars(unifier.getVars(sourceTerm)));
                }
                if(!unifier.isGround(targetTerm)) {
                    return delay(c, Delay.ofVars(unifier.getVars(targetTerm)));
                }
                final Scope source =
                        AScope.matcher().match(sourceTerm, unifier).orElseThrow(() -> new IllegalArgumentException(
                                "Expected source scope, got " + unifier.toString(sourceTerm)));
                final Scope target =
                        AScope.matcher().match(targetTerm, unifier).orElseThrow(() -> new IllegalArgumentException(
                                "Expected target scope, got " + unifier.toString(targetTerm)));
                scopeGraph.addEdge(source, label, target);
                return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS,
                        fuel);
            }

            @Override public Boolean caseTermId(CAstId c) throws InterruptedException {
                final ITerm term = c.astTerm();
                final ITerm idTerm = c.idTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!(unifier.isGround(term))) {
                    return delay(c, Delay.ofVars(unifier.getVars(term)));
                }
                final CEqual eq;
                final Optional<Scope> maybeScope = AScope.matcher().match(term, unifier);
                if(maybeScope.isPresent()) {
                    final AScope scope = maybeScope.get();
                    eq = new CEqual(idTerm, scope);
                    return success(c, state, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES,
                            NO_EXISTENTIALS, fuel);
                } else {
                    final Optional<TermIndex> maybeIndex = TermIndex.find(unifier.findTerm(term));
                    if(maybeIndex.isPresent()) {
                        final ITerm indexTerm = TermOrigin.copy(term, maybeIndex.get());
                        eq = new CEqual(idTerm, indexTerm);
                        return success(c, state, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        return fail(c);
                    }
                }
            }

            @Override public Boolean caseTermProperty(CAstProperty c) throws InterruptedException {
                final ITerm idTerm = c.idTerm();
                final ITerm prop = c.property();
                final ITerm value = c.value();

                final IUniDisunifier unifier = state.unifier();
                if(!(unifier.isGround(idTerm))) {
                    return delay(c, Delay.ofVars(unifier.getVars(idTerm)));
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
                                property = state.termProperties().get(key);
                                if(property.multiplicity().equals(Multiplicity.SINGLETON)
                                        && property.value().equals(value)
                                        && property.value().getAttachments().equals(value.getAttachments())) {
                                    return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                            NO_EXISTENTIALS, fuel);
                                }
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
                    return success(c, newState, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                            NO_EXISTENTIALS, fuel);
                } else {
                    return fail(c);
                }
            }

            @Override public Boolean caseTrue(CTrue c) throws InterruptedException {
                return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS,
                        fuel);
            }

            @Override public Boolean caseTry(CTry c) throws InterruptedException {
                final IDebugContext subDebug = debug.subContext();
                final ITypeCheckerContext<Scope, ITerm, ITerm> subContext = scopeGraph.subContext("try");
                final IState.Immutable subState = state.subState().withResource(subContext.id());
                final StatixSolver subSolver = new StatixSolver(c.constraint(), spec, subState, completeness, subDebug,
                        progress, cancel, subContext, RETURN_ON_FIRST_ERROR);
                final IFuture<SolverResult> subResult = subSolver.entail();
                final K<SolverResult> k = (r, ex, fuel) -> {
                    if(ex != null) {
                        debug.error("try {} failed", ex, c.toString(state.unifier()::toString));
                        return fail(c);
                    } else {
                        try {
                            // check entailment w.r.t. the initial substate, not the current state: otherwise,
                            // some variables may be treated as external while they are not
                            if(Solver.entailed(subState, r, subDebug)) {
                                if(debug.isEnabled(Level.Debug)) {
                                    debug.debug("constraint {} entailed", c.toString(state.unifier()::toString));
                                }
                                return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                        NO_EXISTENTIALS, fuel);
                            } else {
                                if(debug.isEnabled(Level.Debug)) {
                                    debug.debug("constraint {} not entailed", c.toString(state.unifier()::toString));
                                }
                                return fail(c);
                            }
                        } catch(Delay delay) {
                            return delay(c, delay);
                        }
                    }
                };
                return future(c, subResult, k);
            }

            @Override public Boolean caseUser(CUser c) throws InterruptedException {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final LazyDebugContext proxyDebug = new LazyDebugContext(debug);

                final List<Rule> rules = spec.rules().getRules(name);
                // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                final Tuple3<Rule, ApplyResult, Boolean> result;
                if((result = RuleUtil.applyOrderedOne(state.unifier(), rules, args, c, ApplyMode.RELAXED, Safety.UNSAFE)
                        .orElse(null)) == null) {
                    debug.debug("No rule applies");
                    return fail(c);
                }
                final ApplyResult applyResult = result._2();
                if(!result._3()) {
                    final Set<ITermVar> stuckVars = Streams.stream(applyResult.guard())
                            .flatMap(g -> g.domainSet().stream()).collect(CapsuleCollectors.toSet());
                    proxyDebug.debug("Rule delayed (multiple conditional matches)");
                    return delay(c, Delay.ofVars(stuckVars));
                }
                proxyDebug.debug("Rule accepted");
                proxyDebug.commit();
                if(INCREMENTAL_CRITICAL_EDGES && applyResult.criticalEdges() == null) {
                    throw new IllegalArgumentException("Solver only accepts specs with pre-computed critical edges.");
                }
                return success(c, state, NO_UPDATED_VARS, Collections.singletonList(applyResult.body()),
                        applyResult.criticalEdges(), NO_EXISTENTIALS, fuel);
            }

        });

    }

    ///////////////////////////////////////////////////////////////////////////
    // entailment
    ///////////////////////////////////////////////////////////////////////////

    private static IFuture<Boolean> entails(ITypeCheckerContext<Scope, ITerm, ITerm> context, Spec spec,
            IState.Immutable state, IConstraint constraint, ICompleteness.Immutable criticalEdges, IDebugContext debug,
            ICancel cancel, IProgress progress) throws Delay {
        final IDebugContext subDebug = debug.subContext();
        final ITypeCheckerContext<Scope, ITerm, ITerm> subContext = context.subContext("entails");
        final IState.Immutable subState = state.subState().withResource(subContext.id());

        final PreSolveResult preSolveResult;
        if((preSolveResult = Solver.preEntail(subState, criticalEdges, constraint).orElse(null)) == null) {
            return CompletableFuture.completedFuture(false);
        }
        if(preSolveResult.constraints.isEmpty()) {
            return CompletableFuture.completedFuture(Solver.entailed(subState, preSolveResult, subDebug));
        }

        final StatixSolver subSolver =
                new StatixSolver(Constraints.conjoin(preSolveResult.constraints), spec, preSolveResult.state,
                        preSolveResult.criticalEdges, subDebug, progress, cancel, subContext, RETURN_ON_FIRST_ERROR);
        return subSolver.entail().thenCompose(r -> {
            final boolean result;
            try {
                // check entailment w.r.t. the initial substate, not the current state: otherwise,
                // some variables may be treated as external while they are not
                if(Solver.entailed(subState, r, subDebug)) {
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("constraint {} entailed", constraint.toString(state.unifier()::toString));
                    }
                    result = true;
                } else {
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("constraint {} not entailed", constraint.toString(state.unifier()::toString));
                    }
                    result = false;
                }
            } catch(Delay delay) {
                throw new IllegalStateException("Unexpected delay.", delay);
            }
            return CompletableFuture.completedFuture(result);
        });
    }

    private IFuture<Boolean> entails(ITypeCheckerContext<Scope, ITerm, ITerm> subContext, IConstraint constraint,
            ICompleteness.Immutable criticalEdges, ICancel cancel) {
        final IDebugContext subDebug = debug.subContext();
        return absorbDelays(() -> {
            final IState.Immutable subState = state.subState().withResource(subContext.id());

            final PreSolveResult preSolveResult;
            try {
                if((preSolveResult = Solver.preEntail(subState, criticalEdges, constraint).orElse(null)) == null) {
                    return CompletableFuture.completedFuture(false);
                }
            } catch(Delay d) {
                return CompletableFuture.completedExceptionally(d);
            }
            if(preSolveResult.constraints.isEmpty()) {
                return CompletableFuture.completedFuture(Solver.entailed(subState, preSolveResult, subDebug));
            }

            final StatixSolver subSolver = new StatixSolver(Constraints.conjoin(preSolveResult.constraints), spec,
                    preSolveResult.state, preSolveResult.criticalEdges, subDebug, progress, cancel, subContext,
                    RETURN_ON_FIRST_ERROR);
            return subSolver.entail().thenCompose(r -> {
                final boolean result;
                // check entailment w.r.t. the initial substate, not the current state: otherwise,
                // some variables may be treated as external while they are not
                if(Solver.entailed(subState, r, subDebug)) {
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("constraint {} entailed", constraint.toString(state.unifier()::toString));
                    }
                    result = true;
                } else {
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("constraint {} not entailed", constraint.toString(state.unifier()::toString));
                    }
                    result = false;
                }
                return CompletableFuture.completedFuture(result);
            });
        });
    }

    @SuppressWarnings("hiding") private <T> IFuture<T> absorbDelays(Function0<IFuture<T>> f) {
        return f.apply().compose((r, ex) -> {
            if(ex != null) {
                try {
                    throw ex;
                } catch(Delay delay) {
                    if(!delay.criticalEdges().isEmpty()) {
                        debug.error("unsupported delay with critical edges {}", delay);
                        throw new IllegalStateException("unsupported delay with critical edges");
                    }
                    if(delay.vars().isEmpty()) {
                        debug.error("unsupported delay without variables {}", delay);
                        throw new IllegalStateException("unsupported delay without variables");
                    }
                    final CompletableFuture<T> result = new CompletableFuture<>();
                    try {
                        delayAction(() -> {
                            absorbDelays(f).whenComplete(result::complete);
                        }, delay.vars());
                    } catch(InterruptedException ie) {
                        result.completeExceptionally(ie);
                    }
                    return result;
                } catch(Throwable t) {
                    return CompletableFuture.completedExceptionally(t);
                }
            } else {
                return CompletableFuture.completedFuture(r);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Open edges & delayed closes
    ///////////////////////////////////////////////////////////////////////////

    private Set.Transient<CriticalEdge> delayedCloses = CapsuleUtil.transientSet();

    private Set.Immutable<ITerm> getOpenEdges(ITerm varOrScope) {
        // we must include queued edge closes here, to ensure we registered the open
        // edge when the close is released
        final List<EdgeOrData<ITerm>> openEdges =
                Streams.stream(completeness.get(varOrScope, state.unifier())).collect(Collectors.toList());
        final List<EdgeOrData<ITerm>> queuedEdges = M.var().match(varOrScope)
                .map(var -> delayedCloses.stream().filter(e -> state.unifier().equal(var, e.scope()))
                        .map(e -> e.edgeOrData()))
                .orElse(Stream.<EdgeOrData<ITerm>>empty()).collect(Collectors.toList());
        return stream(Iterables.concat(openEdges, queuedEdges)).<ITerm>flatMap(eod -> {
            return eod.match(() -> Stream.<ITerm>empty(), (l) -> Stream.of(l));
        }).collect(CapsuleCollectors.toSet());
    }

    private void closeEdge(CriticalEdge criticalEdge) throws InterruptedException {
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("client {} close edge {}/{}", this, state.unifier().toString(criticalEdge.scope()),
                    criticalEdge.edgeOrData());
        }
        delayedCloses.__insert(criticalEdge);
        delayAction(() -> {
            delayedCloses.__remove(criticalEdge);
            closeGroundEdge(criticalEdge);
        }, state.unifier().getVars(criticalEdge.scope()));
    }

    private void closeGroundEdge(CriticalEdge criticalEdge) {
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("client {} close edge {}/{}", this, state.unifier().toString(criticalEdge.scope()),
                    criticalEdge.edgeOrData());
        }
        final Scope scope = Scope.matcher().match(criticalEdge.scope(), state.unifier())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Expected scope, got " + state.unifier().toString(criticalEdge.scope())));
        // @formatter:off
        criticalEdge.edgeOrData().match(
            () -> {
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
        final Set.Immutable<ITermVar> foreignVars =
                Streams.stream(vars).filter(v -> !state.vars().contains(v)).collect(CapsuleCollectors.toSet());
        if(!foreignVars.isEmpty()) {
            throw new IllegalStateException("Cannot delay on foreign variables: " + foreignVars);
        }
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
    // external data
    ///////////////////////////////////////////////////////////////////////////

    public IFuture<ITerm> getExternalRepresentation(ITerm t) {
        final CompletableFuture<ITerm> f = new CompletableFuture<>();
        try {
            delayAction(() -> {
                f.complete(state.unifier().findRecursive(t));
            }, state.unifier().getVars(t));
        } catch(InterruptedException ex) {
            f.completeExceptionally(ex);
        }
        return f;
    }

    public ITerm internalData(ITerm datum) {
        return state.unifier().findRecursive(datum);
    }

    ///////////////////////////////////////////////////////////////////////////
    // data wf & leq
    ///////////////////////////////////////////////////////////////////////////

    private static class ConstraintDataWF implements DataWf<Scope, ITerm, ITerm>, Serializable {

        private static final long serialVersionUID = 42L;

        private final Spec spec;
        private final Rule constraint;

        private final IState.Immutable state;

        public ConstraintDataWF(Spec spec, Rule constraint) {
            // assume constraint.freeVars().isEmpty()
            this.spec = spec;
            this.constraint = constraint;
            this.state = State.of(); // outer solver state unnecessary, because only applied to ground terms
        }

        @Override public IFuture<Boolean> wf(ITerm datum, ITypeCheckerContext<Scope, ITerm, ITerm> context,
                ICancel cancel) throws InterruptedException {
            try {
                final ApplyResult applyResult;
                // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                if((applyResult = RuleUtil.apply(state.unifier(), constraint, ImmutableList.of(datum), null,
                        ApplyMode.STRICT, Safety.UNSAFE).orElse(null)) == null) {
                    return CompletableFuture.completedFuture(false);
                }

                return entails(context, spec, state, applyResult.body(), applyResult.criticalEdges(),
                        new NullDebugContext(), cancel, new NullProgress());
            } catch(Delay e) {
                throw new IllegalStateException("Unexpected delay.", e);
            }
        }

        @Override public Immutable<Scope> scopes() {
            return Patching.ruleScopes(constraint);
        }

        @Override public DataWf<Scope, ITerm, ITerm> patch(IPatchCollection.Immutable<Scope> patches) {
            return new ConstraintDataWF(spec, Patching.patch(constraint, patches));
        }

        @Override public String toString() {
            return constraint.toString();
        }

    }

    private class ConstraintDataWFInternal implements DataWf<Scope, ITerm, ITerm> {

        // Non-static class that is only used on the unit of the type checker
        // that started the query, and on data from that unit. Implicitly uses
        // solver state from the surrounding object .

        private final Rule constraint;

        public ConstraintDataWFInternal(Rule constraint) {
            this.constraint = constraint;
        }

        @Override public IFuture<Boolean> wf(ITerm datum, ITypeCheckerContext<Scope, ITerm, ITerm> context,
                ICancel cancel) throws InterruptedException {
            return absorbDelays(() -> {
                try {
                    final ApplyResult applyResult;
                    // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                    if((applyResult = RuleUtil.apply(state.unifier(), constraint, ImmutableList.of(datum), null,
                            ApplyMode.STRICT, Safety.UNSAFE).orElse(null)) == null) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return entails(context, applyResult.body(), applyResult.criticalEdges(), cancel);
                } catch(Delay delay) {
                    return CompletableFuture.completedExceptionally(delay);
                }
            });
        }

        @Override public String toString() {
            return constraint.toString(state.unifier()::toString);
        }

    }

    private static class ConstraintDataEquiv implements DataLeq<Scope, ITerm, ITerm>, Serializable {

        private static final long serialVersionUID = 42L;

        private final Spec spec;
        private final Rule constraint;

        private final IState.Immutable state;

        public ConstraintDataEquiv(Spec spec, Rule constraint) {
            // assume constraint.freeVars().isEmpty()
            this.spec = spec;
            this.constraint = constraint;
            this.state = State.of(); // outer solver state unnecessary, because only applied to ground terms
        }

        @Override public IFuture<Boolean> leq(ITerm datum1, ITerm datum2,
                ITypeCheckerContext<Scope, ITerm, ITerm> context, ICancel cancel) throws InterruptedException {
            try {
                final ApplyResult applyResult;
                // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                if((applyResult = RuleUtil.apply(state.unifier(), constraint, ImmutableList.of(datum1, datum2), null,
                        ApplyMode.STRICT, Safety.UNSAFE).orElse(null)) == null) {
                    return CompletableFuture.completedFuture(false);
                }

                return entails(context, spec, state, applyResult.body(), applyResult.criticalEdges(),
                        new NullDebugContext(), cancel, new NullProgress());
            } catch(Delay e) {
                throw new IllegalStateException("Unexpected delay.", e);
            }
        }

        private transient @Nullable IFuture<Boolean> alwaysTrue;

        @Override public IFuture<Boolean> alwaysTrue(ITypeCheckerContext<Scope, ITerm, ITerm> context, ICancel cancel) {
            if(alwaysTrue == null) {
                try {
                    switch(SHADOW_OPTIMIZATION) {
                        case CONTEXT:
                            final Boolean isAlways;
                            if((isAlways = constraint.isAlways().orElse(null)) != null) {
                                alwaysTrue = CompletableFuture.completedFuture(isAlways);
                            } else {
                                final ApplyResult result;
                                final Tuple2<ITermVar, IState.Immutable> d1_state =
                                        state.freshVar(B.newVar(state.resource(), "d1"));
                                final Tuple2<ITermVar, IState.Immutable> d2_state =
                                        d1_state._2().freshVar(B.newVar(state.resource(), "d2"));
                                try {
                                    // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                                    if((result = RuleUtil.apply(d2_state._2().unifier(), constraint,
                                            ImmutableList.of(d1_state._1(), d2_state._1()), null, ApplyMode.STRICT,
                                            Safety.UNSAFE).orElse(null)) == null) {
                                        alwaysTrue = CompletableFuture.completedFuture(false);
                                    } else {
                                        alwaysTrue = entails(context, spec, d2_state._2(), result.body(),
                                                result.criticalEdges(), new NullDebugContext(), cancel,
                                                new NullProgress());
                                    }
                                } catch(Delay e) {
                                    throw new IllegalStateException("Unexpected delay.", e);
                                }
                            }
                            break;
                        case RULE:
                            alwaysTrue = CompletableFuture.completedFuture(constraint.isAlways().orElse(false));
                            break;
                        case NONE:
                        default:
                            alwaysTrue = CompletableFuture.completedFuture(false);
                            break;
                    }
                } catch(InterruptedException e) {
                    return CompletableFuture.completedExceptionally(e);
                }
            }
            return alwaysTrue;
        }

        @Override public String toString() {
            return constraint.toString(state.unifier()::toString);
        }

    }

    private class ConstraintDataEquivInternal implements DataLeq<Scope, ITerm, ITerm> {

        // Non-static class that is only used on the unit of the type checker
        // that started the query, and on data from that unit. Implicitly uses
        // solver state from the surrounding object .

        private final Rule constraint;

        public ConstraintDataEquivInternal(Rule constraint) {
            this.constraint = constraint;
        }

        @Override public IFuture<Boolean> leq(ITerm datum1, ITerm datum2,
                ITypeCheckerContext<Scope, ITerm, ITerm> context, ICancel cancel) throws InterruptedException {
            return absorbDelays(() -> {
                try {
                    final ApplyResult applyResult;
                    // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                    if((applyResult = RuleUtil.apply(state.unifier(), constraint, ImmutableList.of(datum1, datum2),
                            null, ApplyMode.STRICT, Safety.UNSAFE).orElse(null)) == null) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return entails(context, applyResult.body(), applyResult.criticalEdges(), cancel);
                } catch(Delay delay) {
                    return CompletableFuture.completedExceptionally(delay);
                }
            });
        }

        private transient @Nullable IFuture<Boolean> alwaysTrue;

        @Override public IFuture<Boolean> alwaysTrue(ITypeCheckerContext<Scope, ITerm, ITerm> context, ICancel cancel) {
            if(alwaysTrue == null) {
                try {
                    switch(SHADOW_OPTIMIZATION) {
                        case CONTEXT:
                            final Boolean isAlways;
                            if((isAlways = constraint.isAlways().orElse(null)) != null) {
                                alwaysTrue = CompletableFuture.completedFuture(isAlways);
                            } else {
                                alwaysTrue = absorbDelays(() -> {
                                    try {
                                        final ApplyResult result;
                                        final Tuple2<ITermVar, IState.Immutable> d1_state =
                                                state.freshVar(B.newVar(state.resource(), "d1"));
                                        final Tuple2<ITermVar, IState.Immutable> d2_state =
                                                d1_state._2().freshVar(B.newVar(state.resource(), "d2"));
                                        // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                                        if((result = RuleUtil.apply(d2_state._2().unifier(), constraint,
                                                ImmutableList.of(d1_state._1(), d2_state._1()), null, ApplyMode.STRICT,
                                                Safety.UNSAFE).orElse(null)) == null) {
                                            return CompletableFuture.completedFuture(false);
                                        }
                                        return entails(context, spec, state, result.body(), result.criticalEdges(),
                                                new NullDebugContext(), cancel, new NullProgress());
                                    } catch(Delay delay) {
                                        return CompletableFuture.completedExceptionally(delay);
                                    }
                                });
                            }
                            break;
                        case RULE:
                            alwaysTrue = CompletableFuture.completedFuture(constraint.isAlways().orElse(false));
                            break;
                        case NONE:
                        default:
                            alwaysTrue = CompletableFuture.completedFuture(false);
                            break;
                    }
                } catch(InterruptedException e) {
                    return CompletableFuture.completedExceptionally(e);
                }
            }
            return alwaysTrue;
        }

        @Override public String toString() {
            return constraint.toString(state.unifier()::toString);
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // capture
    ///////////////////////////////////////////////////////////////////////////

    public SolverState snapshot() {
        final SolverState.Builder builder = SolverState.builder();
        builder.state(state);
        builder.completeness(completeness);
        builder.addAllConstraints(constraints.active());
        builder.addAllConstraints(pendingConstraints);
        builder.addAllConstraints(constraints.delayed().keySet());
        builder.existentials(existentials);
        builder.updatedVars(updatedVars);
        builder.failed(failed);
        final Set.Immutable<CriticalEdge> closes = delayedCloses.freeze();
        delayedCloses = closes.asTransient();
        builder.delayedCloses(closes);

        return builder.build();
    }

    ///////////////////////////////////////////////////////////////////////////
    // rigidness
    ///////////////////////////////////////////////////////////////////////////

    private boolean isRigid(ITermVar var, IState state) {
        return !state.vars().contains(var);
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

        boolean k(R result, Throwable ex, int fuel) throws InterruptedException;

    }

}
