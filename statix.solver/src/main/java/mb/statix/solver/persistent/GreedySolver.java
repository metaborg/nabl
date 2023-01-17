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
import java.util.Set;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.RateLimitedCancel;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

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
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyMode.Safety;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

class GreedySolver {

    private static final ImmutableSet<ITermVar> NO_UPDATED_VARS = ImmutableSet.of();
    private static final ImmutableList<IConstraint> NO_NEW_CONSTRAINTS = ImmutableList.of();
    private static final mb.statix.solver.completeness.Completeness.Immutable NO_NEW_CRITICAL_EDGES =
            Completeness.Immutable.of();
    private static final ImmutableMap<ITermVar, ITermVar> NO_EXISTENTIALS = ImmutableMap.of();

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
    private Map<ITermVar, ITermVar> existentials = null;
    private final List<ITermVar> updatedVars = Lists.newArrayList();
    private final List<CriticalEdge> removedEdges = Lists.newArrayList();
    private final Map<IConstraint, IMessage> failed = Maps.newHashMap();

    private int solved = 0;
    private int criticalEdges = 0;

    public GreedySolver(Spec spec, IState.Immutable state, IConstraint initialConstraint, IsComplete _isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel, int flags) {
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
        this.flags = flags;
    }

    public GreedySolver(Spec spec, IState.Immutable state, Iterable<IConstraint> constraints,
            Map<IConstraint, Delay> delays, ICompleteness.Immutable completeness, IsComplete _isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel, int flags) {
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
        this.flags = flags;
    }

    public SolverResult solve() throws InterruptedException {
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

    protected SolverResult finishSolve() {
        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.debug("Solved constraints with {} failed and {} remaining constraint(s).", failed.size(),
                constraints.delayedSize());
        if(debug.isEnabled(Level.Debug)) {
            for(Map.Entry<IConstraint, Delay> entry : delayed.entrySet()) {
                debug.debug(" * {} on {}", entry.getKey().toString(state.unifier()::toString), entry.getValue());
            }
        }

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(NO_EXISTENTIALS);
        return SolverResult.of(spec, state, failed, delayed, existentials, updatedVars, removedEdges, completeness)
                .withTotalSolved(solved).withTotalCriticalEdges(criticalEdges);
    }

    ///////////////////////////////////////////////////////////////////////////
    // success/failure signals
    ///////////////////////////////////////////////////////////////////////////

    private boolean success(IConstraint constraint, IState.Immutable newState, Collection<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints, ICompleteness.Immutable newCriticalEdges,
            Map<ITermVar, ITermVar> existentials, int fuel) throws InterruptedException {
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

        // continue on new constraints
        for(IConstraint newConstraint : newConstraints) {
            if(!step(newConstraint, fuel - 1)) {
                return false;
            }
        }

        return true;
    }

    private boolean delay(IConstraint constraint, Delay delay) {
        final IDebugContext subDebug = debug.subContext();
        constraints.delay(constraint, delay);
        if(subDebug.isEnabled(Level.Debug)) {
            subDebug.debug("Delayed: {}", Solver.toString(constraint, state.unifier()));
        }
        return true;
    }

    private boolean fail(IConstraint constraint) {
        final IMessage message = MessageUtil.findClosestMessage(constraint);
        failed.put(constraint, message);
        removeCompleteness(constraint);
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
        this.removedEdges.addAll(removedEdges);
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
            return k(constraint, fuel);
        } catch(InterruptedException | SolverFatalErrorException e) {
            throw e;
        } catch(Throwable e) {
            throw new SolverFatalErrorException(e, constraint, state.unifier(), state.scopeGraph(),
                    Solver.ERROR_TRACE_TERM_DEPTH);
        }
    }

    private boolean k(IConstraint constraint, int fuel) throws InterruptedException {
        cancel.throwIfCancelled();

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
                IDebugContext debug = params.debug();
                IUniDisunifier.Immutable unifier = state.unifier();
                try {
                    final IUniDisunifier.Result<IUnifier.Immutable> result;
                    if((result = unifier.unify(term1, term2, v -> params.isRigid(v, state)).orElse(null)) != null) {
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

            @Override public Boolean caseFalse(CFalse c) {
                return fail(c);
            }

            @Override public Boolean caseInequal(CInequal c) throws InterruptedException {
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
                        final Set<ITermVar> updatedVars =
                                result.result().<Set<ITermVar>>map(Diseq::domainSet).orElse(NO_UPDATED_VARS);
                        return success(c, newState, updatedVars, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        debug.debug("Disunification failed");
                        return fail(c);
                    }
                } catch(RigidException e) {
                    return delay(c, Delay.ofVars(e.vars()));
                }
            }

            @Override public Boolean caseNew(CNew c) throws InterruptedException {
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

                return success(c, newState, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES,
                        NO_EXISTENTIALS, fuel);
            }

            @Override public Boolean caseResolveQuery(IResolveQuery c) throws InterruptedException {
                final QueryFilter filter = c.filter();
                final QueryMin min = c.min();
                final QueryProject project = c.project();
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm resultTerm = c.resultTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return delay(c, Delay.ofVars(unifier.getVars(scopeTerm)));
                }
                final Scope scope;
                // @formatter:off
                final Set<ITermVar> freeVars = Streams.concat(
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

                if((scope = AScope.matcher().match(scopeTerm, unifier).orElse(null)) == null) {
                    debug.error("Expected scope, got {}", unifier.toString(scopeTerm));
                    fail(constraint);
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
                    final Collection<ITerm> pathTerms = Streams.stream(paths)
                            .map(p -> StatixTerms.pathToTerm(p, spec.dataLabels()))
                            .map(p -> project.apply(p).orElseThrow(() -> new IllegalStateException("Invalid resolution path: " + p)))
                            .collect(project.collector());
                    // @formatter:on
                    final IConstraint C = new CEqual(resultTerm, B.newList(pathTerms), c);
                    return success(c, state, NO_UPDATED_VARS, ImmutableList.of(C), NO_NEW_CRITICAL_EDGES,
                            NO_EXISTENTIALS, fuel);
                } catch(IncompleteException e) {
                    params.debug().debug("Query resolution delayed: {}", e.getMessage());
                    return delay(c, Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label())));
                } catch(ResolutionDelayException e) {
                    params.debug().debug("Query resolution delayed: {}", e.getMessage());
                    return delay(c, e.getCause());
                } catch(ResolutionException e) {
                    params.debug().debug("Query resolution failed: {}", e.getMessage());
                    return fail(c);
                }
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
                final Scope source;
                if((source = AScope.matcher().match(sourceTerm, unifier).orElse(null)) == null) {
                    debug.error("Expected source scope, got {}", unifier.toString(sourceTerm));
                    return fail(c);
                }
                if(params.isClosed(source, state)) {
                    return fail(c);
                }
                final Scope target;
                if((target = AScope.matcher().match(targetTerm, unifier).orElse(null)) == null) {
                    debug.error("Expected target scope, got {}", unifier.toString(targetTerm));
                    return fail(c);
                }
                final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                        state.scopeGraph().addEdge(source, label, target);
                return success(c, state.withScopeGraph(scopeGraph), NO_UPDATED_VARS, NO_NEW_CONSTRAINTS,
                        NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS, fuel);
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
                final IDebugContext debug = params.debug();
                try {
                    if(Solver.entails(spec, state, c.constraint(), params::isComplete, new NullDebugContext(),
                            progress.subProgress(1), cancel)) {
                        return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        return fail(c);
                    }
                } catch(Delay delay) {
                    debug.debug("Try delayed: {}", delay.getMessage());
                    return delay(c, delay);
                }
            }

            @Override public Boolean caseUser(CUser c) throws InterruptedException {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final LazyDebugContext proxyDebug = new LazyDebugContext(debug);
                final IDebugContext debug = params.debug();

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


}
