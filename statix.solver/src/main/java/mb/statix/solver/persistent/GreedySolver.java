package mb.statix.solver.persistent;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.constraints.Constraints.disjoin;
import static mb.statix.solver.persistent.Solver.INCREMENTAL_CRITICAL_EDGES;
import static mb.statix.solver.persistent.Solver.RETURN_ON_FIRST_ERROR;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.RateLimitedCancel;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

import io.usethesource.capsule.Set;
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
import mb.scopegraph.oopsla20.INameResolution;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.IncompleteException;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.scopegraph.oopsla20.reference.ResolutionInterpreter;
import mb.statix.constraints.CArith;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.IResolveQuery;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.scopegraph.AScope;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.ConstraintContext;
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
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.query.ConstraintQueries;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.solver.query.QueryProject;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.solver.store.BaseConstraintStore;
import mb.statix.solver.persistent.step.AResolveQueryStep;
import mb.statix.solver.persistent.step.CArithStep;
import mb.statix.solver.persistent.step.CAstIdStep;
import mb.statix.solver.persistent.step.CAstPropertyStep;
import mb.statix.solver.persistent.step.CConjStep;
import mb.statix.solver.persistent.step.CEqualStep;
import mb.statix.solver.persistent.step.CExistsStep;
import mb.statix.solver.persistent.step.CFalseStep;
import mb.statix.solver.persistent.step.CInequalStep;
import mb.statix.solver.persistent.step.CNewStep;
import mb.statix.solver.persistent.step.CTellEdgeStep;
import mb.statix.solver.persistent.step.CTrueStep;
import mb.statix.solver.persistent.step.CTryStep;
import mb.statix.solver.persistent.step.CUserStep;
import mb.statix.solver.persistent.step.IStep;
import mb.statix.solver.persistent.step.StepResult;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyMode.Safety;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

class GreedySolver<TR extends SolverTracer.IResult<TR>> {

    private static final int CANCEL_RATE = 42;
    private static final int MAX_DEPTH = 32;

    // set-up
    private final Spec spec;
    private final IDebugContext debug;
    private final IConstraintStore constraints;
    private final ConstraintContext params;

    private final IProgress progress;
    private final ICancel cancel;
    private final int flags;

    private IState.Immutable state;
    private ICompleteness.Immutable completeness;
    private @Nullable io.usethesource.capsule.Map.Immutable<ITermVar, ITermVar> existentials = null;
    private Set.Transient<ITermVar> updatedVars = CapsuleUtil.transientSet();
    private Set.Transient<CriticalEdge> removedEdges = CapsuleUtil.transientSet();
    private io.usethesource.capsule.Map.Transient<IConstraint, IMessage> failed = CapsuleUtil.transientMap();

    private final SolverTracer<TR> tracer;

    private Set.Immutable<ITermVar> updatedVars() {
        final Set.Immutable<ITermVar> updatedVars = this.updatedVars.freeze();
        this.updatedVars = updatedVars.asTransient();
        return updatedVars;
    }

    private Set.Immutable<CriticalEdge> removedEdges() {
        final Set.Immutable<CriticalEdge> removedEdges = this.removedEdges.freeze();
        this.removedEdges = removedEdges.asTransient();
        return removedEdges;
    }

    private io.usethesource.capsule.Map.Immutable<IConstraint, IMessage> failed() {
        final io.usethesource.capsule.Map.Immutable<IConstraint, IMessage> failed = this.failed.freeze();
        this.failed = failed.asTransient();
        return failed;
    }

    private int solved = 0;
    private int criticalEdges = 0;

    public GreedySolver(Spec spec, IState.Immutable state, IConstraint initialConstraint, IsComplete _isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel, SolverTracer<TR> tracer, int flags) {
        if(INCREMENTAL_CRITICAL_EDGES && !spec.hasPrecomputedCriticalEdges()) {
            debug.warn("Leaving precomputing critical edges to solver may result in duplicate work.");
            this.spec = spec.precomputeCriticalEdges();
        } else {
            this.spec = spec;
        }
        this.state = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        final ICompleteness.Transient _completeness = Completeness.Transient.of();
        if(INCREMENTAL_CRITICAL_EDGES) {
            final Tuple2<IConstraint, ICompleteness.Immutable> initialConstraintAndCriticalEdges =
                    CompletenessUtil.precomputeCriticalEdges(initialConstraint, spec.scopeExtensions());
            constraints.add(initialConstraintAndCriticalEdges._1());
            _completeness.addAll(initialConstraintAndCriticalEdges._2(), state.unifier());
        } else {
            constraints.add(initialConstraint);
            _completeness.add(initialConstraint, spec, state.unifier());
        }
        this.completeness = _completeness.freeze();
        final IsComplete isComplete = (s, l, st) -> {
            return this.completeness.isComplete(s, l, st.unifier()) && _isComplete.test(s, l, st);
        };
        this.params = new ConstraintContext(isComplete, debug);
        this.progress = progress;
        this.cancel = new RateLimitedCancel(cancel, CANCEL_RATE);
        this.tracer = tracer;
        this.flags = flags;
    }

    public GreedySolver(Spec spec, IState.Immutable state, Iterable<IConstraint> constraints,
            Map<IConstraint, Delay> delays, ICompleteness.Immutable completeness, IsComplete _isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel, SolverTracer<TR> tracer, int flags) {
        this.spec = spec;
        this.state = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        this.constraints.addAll(constraints);
        this.constraints.delayAll(delays.entrySet());
        this.completeness = completeness;
        // the constraints should already be reflected in completeness
        final IsComplete isComplete = (s, l, st) -> {
            return this.completeness.isComplete(s, l, st.unifier()) && _isComplete.test(s, l, st);
        };
        this.params = new ConstraintContext(isComplete, debug);
        this.progress = progress;
        this.cancel = new RateLimitedCancel(cancel, CANCEL_RATE);
        this.tracer = tracer;
        this.flags = flags;
    }

    public SolverResult<TR> solve() throws InterruptedException {
        debug.debug("Solving constraints");

        IConstraint constraint;
        while((constraint = constraints.remove()) != null) {
            if(!step(constraint, MAX_DEPTH)) {
                debug.debug("Finished fast.");
                return finishSolve();
            }
        }

        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Expected no remaining active constraints, but got ", constraints.activeSize());
        }

        return finishSolve();
    }

    protected SolverResult<TR> finishSolve() {
        final io.usethesource.capsule.Map.Immutable<IConstraint, Delay> delayed = constraints.delayed();
        debug.debug("Solved constraints with {} failed and {} remaining constraint(s).", failed.size(),
                constraints.delayedSize());
        if(debug.isEnabled(Level.Debug)) {
            for(Map.Entry<IConstraint, Delay> entry : delayed.entrySet()) {
                debug.debug(" * {} on {}", entry.getKey().toString(state.unifier()::toString), entry.getValue());
            }
        }

        final io.usethesource.capsule.Map.Immutable<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(Solver.NO_EXISTENTIALS);
        return SolverResult.of(spec, state, tracer.result(state), failed(), delayed, existentials, updatedVars(), removedEdges(), completeness)
                .withTotalSolved(solved).withTotalCriticalEdges(criticalEdges);
    }

    ///////////////////////////////////////////////////////////////////////////
    // success/failure signals
    ///////////////////////////////////////////////////////////////////////////

    private boolean success(IConstraint constraint, IState.Immutable newState, Set.Immutable<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints, ICompleteness.Immutable newCriticalEdges,
            io.usethesource.capsule.Map.Immutable<ITermVar, ITermVar> existentials, int fuel) throws InterruptedException {
        solved += 1;

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
            this.updatedVars.__insertAll(updatedVars);
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

        // continue on new constraints
        for(IConstraint newConstraint : newConstraints) {
            if(!step(newConstraint, fuel - 1)) {
                return false;
            }
        }

        tracer.onConstraintSolved(constraint, newState);

        return true;
    }

    private boolean delay(IConstraint constraint, Delay delay) {
        final IDebugContext subDebug = debug.subContext();
        constraints.delay(constraint, delay);
        if(subDebug.isEnabled(Level.Debug)) {
            subDebug.debug("Delayed: {}", Solver.toString(constraint, state.unifier()));
        }
        tracer.onConstraintDelayed(constraint, state);
        return true;
    }

    private boolean fail(IConstraint constraint) {
        final IMessage message = MessageUtil.findClosestMessage(constraint);
        failed.__put(constraint, message);
        removeCompleteness(constraint);
        tracer.onConstraintFailed(constraint, state);
        return message.kind() != MessageKind.ERROR || (flags & RETURN_ON_FIRST_ERROR) == 0;
    }

    private void removeCompleteness(IConstraint constraint) {
        final Set<CriticalEdge> removedEdges;
        final ICompleteness.Transient _completeness = completeness.melt();
        if(INCREMENTAL_CRITICAL_EDGES) {
            if(!constraint.ownCriticalEdges().isPresent()) {
                throw new IllegalArgumentException("Solver only accepts constraints with pre-computed critical edges.");
            }
            criticalEdges +=
                    constraint.ownCriticalEdges().get().entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
            removedEdges = _completeness.removeAll(constraint.ownCriticalEdges().get(), state.unifier());
        } else {
            removedEdges = _completeness.remove(constraint, spec, state.unifier());
        }
        this.completeness = _completeness.freeze();
        constraints.activateFromEdges(removedEdges, debug);
        this.removedEdges.__insertAll(removedEdges);
    }

    private boolean queue(IConstraint constraint) {
        constraints.add(constraint);
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // k
    ///////////////////////////////////////////////////////////////////////////

    private boolean step(IConstraint constraint, int fuel) throws InterruptedException {
        try {
            if(fuel <= 0) {
                return queue(constraint);
            }
            final IStep step = k(constraint);
            return applyStep(step, tracer.onStep(step, state), fuel);
        } catch(InterruptedException | SolverFatalErrorException e) {
            throw e;
        } catch(Throwable e) {
            throw new SolverFatalErrorException(e, constraint, state.unifier(), state.scopeGraph(),
                    Solver.ERROR_TRACE_TERM_DEPTH);
        }
    }

    private boolean applyStep(IStep step, Optional<StepResult> stepResultOverride, int fuel) throws InterruptedException {
        stepResultOverride.ifPresent(stepResult -> debug.debug("result override by tracer: {} (was {})", stepResult, step.result()));
        return stepResultOverride.orElse(step.result()).match(
                (newState, updatedVars, newConstraints, newCriticalEdges, newExistentials) ->
                        success(step.constraint(), newState, updatedVars, newConstraints, newCriticalEdges, newExistentials, fuel),
                ex -> fail(step.constraint()),
                delay -> delay(step.constraint(), delay)
        );
    }

    private IStep k(IConstraint constraint) throws InterruptedException {
        cancel.throwIfCancelled();

        // stop recursion if we run out of fuel

        tracer.onTrySolveConstraint(constraint, state);
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Solving {}",
                    constraint.toString(Solver.shallowTermFormatter(state.unifier(), Solver.TERM_FORMAT_DEPTH)));
        }

        // solve
        return constraint.matchOrThrow(new IConstraint.CheckedCases<IStep, InterruptedException>() {

            @Override public IStep caseArith(CArith c) {
                final IUniDisunifier unifier = state.unifier();
                final Optional<ITerm> term1 = c.expr1().isTerm();
                final Optional<ITerm> term2 = c.expr2().isTerm();
                try {
                    if(c.op().isEquals() && term1.isPresent()) {
                        int i2 = c.expr2().eval(unifier);
                        final IConstraint eq = new CEqual(term1.get(), B.newInt(i2), c);
                        return CArithStep.of(c, StepResult.success(state).withNewConstraints(ImList.Immutable.of(eq)));
                    } else if(c.op().isEquals() && term2.isPresent()) {
                        int i1 = c.expr1().eval(unifier);
                        final IConstraint eq = new CEqual(B.newInt(i1), term2.get(), c);
                        return CArithStep.of(c, StepResult.success(state).withNewConstraints(ImList.Immutable.of(eq)));
                    } else {
                        int i1 = c.expr1().eval(unifier);
                        int i2 = c.expr2().eval(unifier);
                        if(c.op().test(i1, i2)) {
                            return CArithStep.of(c, StepResult.success(state));
                        } else {
                            return CArithStep.of(c, StepResult.failure());
                        }
                    }
                } catch(Delay d) {
                    return CArithStep.of(c, StepResult.delay(d));
                }
            }

            @Override public IStep caseConj(CConj c) {
                return CConjStep.of(c, StepResult.success(state).withNewConstraints(disjoin(c)));
            }

            @Override public IStep caseEqual(CEqual c) {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IDebugContext debug = params.debug();
                IUniDisunifier.Immutable unifier = state.unifier();
                try {
                    final IUniDisunifier.Result<IUnifier.Immutable> result;
                    if((result = unifier.unify(term1, term2, v -> params.isRigid(v, state)).orElse(null)) != null) {
                        if(debug.isEnabled(Level.Debug)) {
                            debug.debug("Unification succeeded: {}", result.result());
                        }
                        final IState.Immutable newState = state.withUnifier(result.unifier());
                        final Set.Immutable<ITermVar> updatedVars = result.result().domainSet();
                        return CEqualStep.of(c, StepResult.success(newState).withUpdatedVars(updatedVars), result);
                    } else {
                        if(debug.isEnabled(Level.Debug)) {
                            debug.debug("Unification failed: {} != {}", unifier.toString(term1),
                                    unifier.toString(term2));
                        }
                        return CEqualStep.of(c, StepResult.failure(), null);
                    }
                } catch(OccursException e) {
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
                    }
                    return CEqualStep.of(c, StepResult.failure(e), null);
                } catch(RigidException e) {
                    return CEqualStep.of(c, StepResult.delay(Delay.ofVars(e.vars())), null);
                }
            }

            @Override public IStep caseExists(CExists c) {
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
                final IConstraint newConstraint = c.constraint().apply(subst, true).withCause(c.cause().orElse(null));
                if(INCREMENTAL_CRITICAL_EDGES && !c.bodyCriticalEdges().isPresent()) {
                    throw new IllegalArgumentException(
                            "Solver only accepts constraints with pre-computed critical edges.");
                }
                final ICompleteness.Immutable newCriticalEdges =
                        c.bodyCriticalEdges().orElse(Solver.NO_NEW_CRITICAL_EDGES).apply(subst);
                final io.usethesource.capsule.Map.Immutable<ITermVar, ITermVar> newExistentials = existentials.asMap();
                return CExistsStep.of(c, StepResult.success(newState)
                        .withNewConstraints(ImList.Immutable.of(newConstraint))
                        .withNewCriticalEdges(newCriticalEdges)
                        .withNewExistentials(newExistentials),
                        newExistentials);
            }

            @Override public IStep caseFalse(CFalse c) {
                return CFalseStep.of(c, StepResult.failure());
            }

            @Override public IStep caseInequal(CInequal c) {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IDebugContext debug = params.debug();
                final IUniDisunifier.Immutable unifier = state.unifier();
                try {
                    final IUniDisunifier.Result<Optional<Diseq>> result;
                    if((result = unifier.disunify(c.universals(), term1, term2, v -> params.isRigid(v, state))
                            .orElse(null)) != null) {
                        if(debug.isEnabled(Level.Debug)) {
                            debug.debug("Disunification succeeded: {}", result);
                        }
                        final IState.Immutable newState = state.withUnifier(result.unifier());
                        final Set.Immutable<ITermVar> updatedVars =
                                result.result().map(Diseq::domainSet).orElse(Solver.NO_UPDATED_VARS);
                        return CInequalStep.of(c, StepResult.success(newState).withUpdatedVars(updatedVars), result);
                    } else {
                        debug.debug("Disunification failed");
                        return CInequalStep.of(c, StepResult.failure(), null);
                    }
                } catch(RigidException e) {
                    return CInequalStep.of(c, StepResult.delay(Delay.ofVars(e.vars())), null);
                }
            }

            @Override public IStep caseNew(CNew c) {
                IState.Immutable newState = state;

                final ITerm scopeTerm = c.scopeTerm();
                final String base = M.var(ITermVar::getName).match(scopeTerm).orElse("s");
                final Tuple2<Scope, IState.Immutable> ss = newState.freshScope(base);
                final Scope scope = ss._1();
                newState = ss._2();

                final ITerm datumTerm = c.datumTerm();
                final IScopeGraph.Immutable<Scope, ITerm, ITerm> newScopeGraph =
                        state.scopeGraph().setDatum(scope, datumTerm);
                newState = newState.withScopeGraph(newScopeGraph);

                final IConstraint eq = new CEqual(scopeTerm, scope, c);

                return CNewStep.of(c, StepResult.success(newState).withNewConstraints(ImList.Immutable.of(eq)), scope, datumTerm);
            }

            @Override public IStep caseResolveQuery(IResolveQuery c) throws InterruptedException {
                final QueryFilter filter = c.filter();
                final QueryMin min = c.min();
                final QueryProject project = c.project();
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm resultTerm = c.resultTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return AResolveQueryStep.of(c, StepResult.delay(Delay.ofVars(unifier.getVars(scopeTerm))), null);
                }
                final Scope scope;
                final io.usethesource.capsule.Set.Transient<ITermVar> freeVarsBuilder = unifier.getVars(scopeTerm).asTransient();
                filter.getDataWF().freeVars().stream().map(v -> unifier.getVars(v)).forEach(freeVarsBuilder::__insertAll);
                min.getDataEquiv().freeVars().stream().map(v -> unifier.getVars(v)).forEach(freeVarsBuilder::__insertAll);
                final io.usethesource.capsule.Set.Immutable<ITermVar> freeVars = freeVarsBuilder.freeze();
                if(!freeVars.isEmpty()) {
                    return AResolveQueryStep.of(c, StepResult.delay(Delay.ofVars(freeVars)), null);
                }
                final Rule dataWfRule = RuleUtil.instantiateHeadPatterns(
                        RuleUtil.closeInUnifier(filter.getDataWF(), state.unifier(), Safety.UNSAFE));
                final Rule dataLeqRule = RuleUtil.instantiateHeadPatterns(
                        RuleUtil.closeInUnifier(min.getDataEquiv(), state.unifier(), Safety.UNSAFE));

                if((scope = AScope.matcher().match(scopeTerm, unifier).orElse(null)) == null) {
                    final String scopeTermString = unifier.toString(scopeTerm);
                    debug.error("Expected scope, got {}", scopeTermString);
                    return AResolveQueryStep.of(c, StepResult.failure(new IllegalStateException(scopeTermString + " is not a scope")), null);
                }

                try {
                    final ConstraintQueries cq = new ConstraintQueries(spec, state, params::isComplete);
                    // @formatter:off
                    final Env<Scope, ITerm, ITerm> paths = c.matchInResolution(
                        resolveQuery -> {
                            final INameResolution<Scope, ITerm, ITerm> nameResolution = Solver.nameResolutionBuilder()
                                    .withLabelWF(cq.getLabelWF(filter.getLabelWF()))
                                    .withDataWF(cq.getDataWF(dataWfRule))
                                    .withLabelOrder(cq.getLabelOrder(min.getLabelOrder()))
                                    .withDataEquiv(cq.getDataEquiv(dataLeqRule))
                                    .withIsComplete((s, l) -> params.isComplete(s, l, state))
                                    .build(state.scopeGraph(), spec.allLabels());
                            return nameResolution.resolve(scope, cancel);
                        },
                        compiledQuery -> {
                            final ResolutionInterpreter<Scope, ITerm, ITerm> interpreter =
                                new ResolutionInterpreter<>(state.scopeGraph(), cq.getDataWF(dataWfRule),
                                cq.getDataEquiv(dataLeqRule), compiledQuery.stateMachine(), (s, l) -> params.isComplete(s, l, state));
                            return interpreter.resolve(scope, cancel);
                        }
                    );
                    // @formatter:on

                    // @formatter:off
                    final Collection<ITerm> pathTerms = paths.stream()
                            .map(p -> StatixTerms.pathToTerm(p, spec.dataLabels()))
                            .map(p -> project.apply(p).<IllegalStateException>orElseThrow(() -> new IllegalStateException("Invalid resolution path: " + p)))
                            .collect(project.collector());
                    // @formatter:on
                    final IConstraint C = new CEqual(resultTerm, B.newList(pathTerms), c);
                    return AResolveQueryStep.of(c, StepResult.success(state).withNewConstraints(ImList.Immutable.of(C)), paths);
                } catch(IncompleteException e) {
                    params.debug().debug("Query resolution delayed: {}", e.getMessage());
                    return AResolveQueryStep.of(c, StepResult.delay(Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label()))), null);
                } catch(ResolutionDelayException e) {
                    params.debug().debug("Query resolution delayed: {}", e.getMessage());
                    return AResolveQueryStep.of(c, StepResult.delay(e.getCause()), null);
                } catch(ResolutionException e) {
                    params.debug().debug("Query resolution failed: {}", e.getMessage());
                    return AResolveQueryStep.of(c, StepResult.failure(e), null);
                }
            }

            @Override public IStep caseTellEdge(CTellEdge c) {
                final ITerm sourceTerm = c.sourceTerm();
                final ITerm label = c.label();
                final ITerm targetTerm = c.targetTerm();

                final IUniDisunifier unifier = state.unifier();
                final io.usethesource.capsule.Set.Transient<ITermVar> freeVars = unifier.getVars(sourceTerm).asTransient();
                freeVars.__insertAll(unifier.getVars(targetTerm));
                if(!freeVars.isEmpty()) {
                    return CTellEdgeStep.of(c, StepResult.delay(Delay.ofVars(freeVars.freeze())), null, null);
                }
                final Scope source;
                if((source = AScope.matcher().match(sourceTerm, unifier).orElse(null)) == null) {
                    String scopeTermString = unifier.toString(sourceTerm);
                    debug.error("Expected source scope, got {}", scopeTermString);
                    return CTellEdgeStep.of(c, StepResult.failure(new IllegalStateException(scopeTermString + " is not a scope")), null, null);
                }
                if(params.isClosed(source, state)) {
                    return CTellEdgeStep.of(c, StepResult.failure(new IllegalStateException(source + " is closed")), null, null);
                }
                final Scope target;
                if((target = AScope.matcher().match(targetTerm, unifier).orElse(null)) == null) {
                    String scopeTermString = unifier.toString(targetTerm);
                    debug.error("Expected target scope, got {}", scopeTermString);
                    return CTellEdgeStep.of(c, StepResult.failure(new IllegalStateException(scopeTermString + " is not a scope")), null, null);
                }
                final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                        state.scopeGraph().addEdge(source, label, target);
                final IState.Immutable newState = state.withScopeGraph(scopeGraph);
                return CTellEdgeStep.of(c, StepResult.success(newState), source, target);
            }

            @Override public IStep caseTermId(CAstId c) {
                final ITerm term = c.astTerm();
                final ITerm idTerm = c.idTerm();

                final IUniDisunifier unifier = state.unifier();
                final Set.Immutable<ITermVar> vars = unifier.getVars(term);
                if(!vars.isEmpty()) {
                    return CAstIdStep.of(c, StepResult.delay(Delay.ofVars(vars)), null);
                }
                final CEqual eq;
                final ITerm index;
                final Optional<Scope> maybeScope = AScope.matcher().match(term, unifier);
                if(maybeScope.isPresent()) {
                    final AScope scope = maybeScope.get();
                    eq = new CEqual(idTerm, scope);
                    index = scope;
                } else {
                    final Optional<TermIndex> maybeIndex = TermIndex.find(unifier.findTerm(term));
                    if(maybeIndex.isPresent()) {
                        final ITerm indexTerm = TermOrigin.copy(term, maybeIndex.get());
                        eq = new CEqual(idTerm, indexTerm);
                        index = indexTerm;
                    } else {
                        return CAstIdStep.of(c, StepResult.failure(), null);
                    }
                }
                return CAstIdStep.of(c, StepResult.success(state).withNewConstraints(ImList.Immutable.of(eq)), index);
            }

            @Override public IStep caseTermProperty(CAstProperty c) {
                final ITerm idTerm = c.idTerm();
                final ITerm prop = c.property();
                final ITerm value = c.value();

                final IUniDisunifier unifier = state.unifier();
                final Set.Immutable<ITermVar> vars = unifier.getVars(idTerm);
                if(!vars.isEmpty()) {
                    return CAstPropertyStep.of(c, StepResult.delay(Delay.ofVars(vars)), null, null, null);
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
                                return CAstPropertyStep.of(c, StepResult.failure(), null, null, null);
                            }
                            property = property.addValue(value);
                            break;
                        }
                        case SET: {
                            if(state.termProperties().containsKey(key)) {
                                property = state.termProperties().get(key);
                                if(!property.multiplicity().equals(Multiplicity.SINGLETON)) {
                                    return CAstPropertyStep.of(c, StepResult.failure(), null, null, null);
                                }
                                final ITerm propVal = unifier.findRecursive(property.value());
                                final ITerm newVal = unifier.findRecursive(value);
                                if(propVal.equals(newVal) && propVal.getAttachments().equals(newVal.getAttachments())) {
                                    // no state update, early return
                                    return CAstPropertyStep.of(c, StepResult.success(state), index, property, newVal).withUpdate(false);
                                } else {
                                    return CAstPropertyStep.of(c, StepResult.failure(), null, null, null);
                                }
                            }
                            property = SingletonTermProperty.of(value);
                            break;
                        }
                        default:
                            throw new IllegalStateException("Unknown op " + c.op());
                    }
                    final IState.Immutable newState =
                            state.withTermProperties(state.termProperties().__put(key, property));
                    return CAstPropertyStep.of(c, StepResult.success(newState), index, property, value);
                } else {
                    return CAstPropertyStep.of(c, StepResult.failure(), null, null, null);
                }
            }

            @Override public IStep caseTrue(CTrue c) {
                return CTrueStep.of(c, StepResult.success(state));
            }

            @Override public IStep caseTry(CTry c) throws InterruptedException {
                final IDebugContext debug = params.debug();
                try {
                    if(Solver.entails(spec, state, c.constraint(), params::isComplete, new NullDebugContext(),
                            progress.subProgress(1), cancel)) {
                        return CTryStep.of(c, StepResult.success(state));
                    } else {
                        return CTryStep.of(c, StepResult.failure());
                    }
                } catch(Delay delay) {
                    debug.debug("Try delayed: {}", delay.getMessage());
                    return CTryStep.of(c, StepResult.delay(delay));
                }
            }

            @Override public IStep caseUser(CUser c) {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final LazyDebugContext proxyDebug = new LazyDebugContext(debug);
                final IDebugContext debug = params.debug();

                final ImList.Immutable<Rule> rules = spec.rules().getRules(name);
                // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
                final Tuple3<Rule, ApplyResult, Boolean> result;
                if((result = RuleUtil.applyOrderedOne(state.unifier(), rules, args, c, ApplyMode.RELAXED, Safety.UNSAFE, true)
                        .orElse(null)) == null) {
                    debug.debug("No rule applies");
                    return CUserStep.of(c, StepResult.failure(), null);
                }
                final ApplyResult applyResult = result._2();
                if(!result._3()) {
                    final Set<ITermVar> stuckVars = applyResult.guard()
                            .map(Diseq::domainSet).orElse(CapsuleUtil.immutableSet());
                    proxyDebug.debug("Rule delayed (multiple conditional matches)");
                    return CUserStep.of(c, StepResult.delay(Delay.ofVars(stuckVars)), applyResult);
                }
                proxyDebug.debug("Rule accepted");
                proxyDebug.commit();
                if(INCREMENTAL_CRITICAL_EDGES && applyResult.criticalEdges() == null) {
                    throw new IllegalArgumentException("Solver only accepts specs with pre-computed critical edges.");
                }
                return CUserStep.of(c, StepResult.success(state).withNewConstraints(Collections.singletonList(applyResult.body()))
                        .withNewCriticalEdges(applyResult.criticalEdges()), applyResult);
            }

        });

    }


}
