package mb.statix.solver.persistent;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.constraints.Constraints.disjoin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.RateLimitedCancel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.Diseq;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.Tuple2;
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
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.IncompleteException;
import mb.statix.scopegraph.reference.ResolutionException;
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
import mb.statix.solver.completeness.CompletenessUtil;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.query.ConstraintQueries;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.solver.store.BaseConstraintStore;
import mb.statix.spec.ApplyMode;
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
    private final ICompleteness.Transient completeness;
    private final IState.Immutable initialState;
    private final ConstraintContext params;

    private final IProgress progress;
    private final ICancel cancel;

    private Map<ITermVar, ITermVar> existentials = null;
    private final List<ITermVar> updatedVars = Lists.newArrayList();
    private final List<CriticalEdge> removedEdges = Lists.newArrayList();
    private final Map<IConstraint, IMessage> failed = Maps.newHashMap();

    private int solved = 0;
    private int criticalEdges = 0;

    public GreedySolver(Spec spec, IState.Immutable state, IConstraint initialConstraint, IsComplete _isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel) {
        if(Solver.INCREMENTAL_CRITICAL_EDGES && !spec.hasPrecomputedCriticalEdges()) {
            debug.warn("Leaving precomputing critical edges to solver may result in duplicate work.");
            this.spec = spec.precomputeCriticalEdges();
        } else {
            this.spec = spec;
        }
        this.initialState = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        if(Solver.INCREMENTAL_CRITICAL_EDGES) {
            final Tuple2<IConstraint, ICompleteness.Immutable> initialConstraintAndCriticalEdges =
                    CompletenessUtil.precomputeCriticalEdges(initialConstraint, spec.scopeExtensions());
            constraints.add(initialConstraintAndCriticalEdges._1());
            this.completeness = Completeness.Transient.of();
            completeness.addAll(initialConstraintAndCriticalEdges._2(), initialState.unifier());
        } else {
            constraints.add(initialConstraint);
            this.completeness = Completeness.Transient.of();
            completeness.add(initialConstraint, spec, initialState.unifier());
        }
        final IsComplete isComplete = (s, l, st) -> {
            return completeness.isComplete(s, l, st.unifier()) && _isComplete.test(s, l, st);
        };
        this.params = new ConstraintContext(isComplete, debug);
        this.progress = progress;
        this.cancel = new RateLimitedCancel(cancel, CANCEL_RATE);
    }

    public GreedySolver(Spec spec, IState.Immutable state, Iterable<IConstraint> constraints,
            Map<IConstraint, Delay> delays, ICompleteness.Immutable completeness, IsComplete _isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel) {
        this.spec = spec;
        this.initialState = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        this.constraints.addAll(constraints);
        this.constraints.delayAll(delays.entrySet());
        this.completeness = completeness.melt();
        // the constraints should already be reflected in completeness
        final IsComplete isComplete = (s, l, st) -> {
            return this.completeness.isComplete(s, l, st.unifier()) && _isComplete.test(s, l, st);
        };
        this.params = new ConstraintContext(isComplete, debug);
        this.progress = progress;
        this.cancel = new RateLimitedCancel(cancel, CANCEL_RATE);
    }

    public SolverResult solve() throws InterruptedException {
        debug.debug("Solving constraints");

        IState.Immutable state = this.initialState;

        IConstraint constraint;
        while((constraint = constraints.remove()) != null) {
            state = k(state, constraint, MAX_DEPTH);
        }

        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Expected no remaining active constraints, but got ", constraints.activeSize());
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.debug("Solved constraints with {} failed and {} remaining constraint(s).", failed.size(),
                constraints.delayedSize());
        if(debug.isEnabled(Level.Debug)) {
            for(Delay delayedConstraint : delayed.values()) {
                debug.debug(" * {}", delayedConstraint.toString());
            }
        }

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(NO_EXISTENTIALS);
        return SolverResult.of(state, failed, delayed, existentials, updatedVars, removedEdges, completeness.freeze())
                .withTotalSolved(solved).withTotalCriticalEdges(criticalEdges);
    }

    private IState.Immutable success(IConstraint constraint, IState.Immutable state, Collection<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints, ICompleteness.Immutable newCriticalEdges,
            Map<ITermVar, ITermVar> existentials, int fuel) throws InterruptedException {
        solved += 1;

        final IDebugContext subDebug = debug.subContext();
        if(this.existentials == null) {
            this.existentials = existentials;
        }
        final IUniDisunifier.Immutable unifier = state.unifier();

        // updates from unified variables
        completeness.updateAll(updatedVars, unifier);
        constraints.activateFromVars(updatedVars, debug);
        this.updatedVars.addAll(updatedVars);

        // add new constraints
        // no constraints::addAll, instead recurse immediately below
        if(Solver.INCREMENTAL_CRITICAL_EDGES) {
            completeness.addAll(newCriticalEdges, unifier); // must come before ICompleteness::remove
        } else {
            completeness.addAll(newConstraints, spec, unifier); // must come before ICompleteness::remove
        }
        if(subDebug.isEnabled(Level.Debug) && !newConstraints.isEmpty()) {
            subDebug.debug("Simplified to:");
            for(IConstraint newConstraint : newConstraints) {
                subDebug.debug(" * {}", Solver.toString(newConstraint, unifier));
            }
        }

        removeCompleteness(constraint, state);

        // continue on new constraints
        for(IConstraint newConstraint : newConstraints) {
            state = k(state, newConstraint, fuel - 1);
        }

        return state;
    }

    private IState.Immutable fail(IConstraint constraint, IState.Immutable state) {
        failed.put(constraint, MessageUtil.findClosestMessage(constraint));
        removeCompleteness(constraint, state);
        return state;
    }

    private IState.Immutable delay(IConstraint constraint, IState.Immutable state, Delay delay) {
        final IDebugContext subDebug = debug.subContext();
        constraints.delay(constraint, delay);
        if(subDebug.isEnabled(Level.Debug)) {
            subDebug.debug("Delayed: {}", Solver.toString(constraint, state.unifier()));
        }
        return state;
    }

    private void removeCompleteness(IConstraint constraint, IState.Immutable state) {
        final Set<CriticalEdge> removedEdges;
        if(Solver.INCREMENTAL_CRITICAL_EDGES) {
            if(!constraint.ownCriticalEdges().isPresent()) {
                throw new IllegalArgumentException("Solver only accepts constraints with pre-computed critical edges.");
            }
            criticalEdges +=
                    constraint.ownCriticalEdges().get().entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
            removedEdges = completeness.removeAll(constraint.ownCriticalEdges().get(), state.unifier());
        } else {
            removedEdges = completeness.remove(constraint, spec, state.unifier());
        }
        constraints.activateFromEdges(removedEdges, debug);
        this.removedEdges.addAll(removedEdges);
    }

    private IState.Immutable queue(IConstraint constraint, IState.Immutable state) {
        constraints.add(constraint);
        return state;
    }

    private IState.Immutable k(IState.Immutable state, IConstraint constraint, int fuel) throws InterruptedException {
        cancel.throwIfCancelled();

        // stop recursion if we run out of fuel
        if(fuel <= 0) {
            return queue(constraint, state);
        }

        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Solving {}", constraint.toString(Solver.shallowTermFormatter(state.unifier())));
        }

        // solve
        return constraint.matchOrThrow(new IConstraint.CheckedCases<IState.Immutable, InterruptedException>() {

            @Override public IState.Immutable caseArith(CArith c) throws InterruptedException {
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
                            return fail(c, state);
                        }
                    }
                } catch(Delay d) {
                    return delay(c, state, d);
                }
            }

            @Override public IState.Immutable caseConj(CConj c) throws InterruptedException {
                final List<IConstraint> newConstraints = disjoin(c);
                return success(c, state, NO_UPDATED_VARS, newConstraints, NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS, fuel);
            }

            @Override public IState.Immutable caseEqual(CEqual c) throws InterruptedException {
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
                        return fail(c, state);
                    }
                } catch(OccursException e) {
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
                    }
                    return fail(c, state);
                } catch(RigidException e) {
                    return delay(c, state, Delay.ofVars(e.vars()));
                }
            }

            @Override public IState.Immutable caseExists(CExists c) throws InterruptedException {
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
                if(Solver.INCREMENTAL_CRITICAL_EDGES && !c.bodyCriticalEdges().isPresent()) {
                    throw new IllegalArgumentException(
                            "Solver only accepts constraints with pre-computed critical edges.");
                }
                final ICompleteness.Immutable newCriticalEdges =
                        c.bodyCriticalEdges().orElse(NO_NEW_CRITICAL_EDGES).apply(subst);
                return success(c, newState, NO_UPDATED_VARS, disjoin(newConstraint), newCriticalEdges, existentials,
                        fuel);
            }

            @Override public IState.Immutable caseFalse(CFalse c) {
                return fail(c, state);
            }

            @Override public IState.Immutable caseInequal(CInequal c) throws InterruptedException {
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
                                result.result().<Set<ITermVar>>map(Diseq::domainSet).orElse(ImmutableSet.of());
                        return success(c, newState, updatedVars, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        debug.debug("Disunification failed");
                        return fail(c, state);
                    }
                } catch(RigidException e) {
                    return delay(c, state, Delay.ofVars(e.vars()));
                }
            }

            @Override public IState.Immutable caseNew(CNew c) throws InterruptedException {
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

            @Override public IState.Immutable caseResolveQuery(CResolveQuery c) throws InterruptedException {
                final QueryFilter filter = c.filter();
                final QueryMin min = c.min();
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm resultTerm = c.resultTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(scopeTerm)));
                }
                final Scope scope;
                if((scope = AScope.matcher().match(scopeTerm, unifier).orElse(null)) == null) {
                    debug.error("Expected scope, got {}", unifier.toString(scopeTerm));
                    fail(constraint, state);
                }

                try {
                    final ConstraintQueries cq = new ConstraintQueries(spec, state, params, progress, cancel);
                    // @formatter:off
                    final INameResolution<Scope, ITerm, ITerm> nameResolution = Solver.nameResolutionBuilder()
                                .withLabelWF(cq.getLabelWF(filter.getLabelWF()))
                                .withDataWF(cq.getDataWF(filter.getDataWF()))
                                .withLabelOrder(cq.getLabelOrder(min.getLabelOrder()))
                                .withDataEquiv(cq.getDataEquiv(min.getDataEquiv()))
                                .withIsComplete((s, l) -> params.isComplete(s, l, state))
                                .build(state.scopeGraph(), spec.allLabels());
                    // @formatter:on
                    final Env<Scope, ITerm, ITerm> paths = nameResolution.resolve(scope, cancel);
                    final List<ITerm> pathTerms =
                            Streams.stream(paths).map(p -> StatixTerms.pathToTerm(p, spec.dataLabels()))
                                    .collect(ImmutableList.toImmutableList());
                    final IConstraint C = new CEqual(resultTerm, B.newList(pathTerms), c);
                    return success(c, state, NO_UPDATED_VARS, ImmutableList.of(C), NO_NEW_CRITICAL_EDGES,
                            NO_EXISTENTIALS, fuel);
                } catch(IncompleteException e) {
                    params.debug().debug("Query resolution delayed: {}", e.getMessage());
                    return delay(c, state, Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label())));
                } catch(ResolutionDelayException e) {
                    params.debug().debug("Query resolution delayed: {}", e.getMessage());
                    return delay(c, state, e.getCause());
                } catch(ResolutionException e) {
                    params.debug().debug("Query resolution failed: {}", e.getMessage());
                    return fail(c, state);
                }
            }

            @Override public IState.Immutable caseTellEdge(CTellEdge c) throws InterruptedException {
                final ITerm sourceTerm = c.sourceTerm();
                final ITerm label = c.label();
                final ITerm targetTerm = c.targetTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(sourceTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(sourceTerm)));
                }
                if(!unifier.isGround(targetTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(targetTerm)));
                }
                final Scope source;
                if((source = AScope.matcher().match(sourceTerm, unifier).orElse(null)) == null) {
                    debug.error("Expected source scope, got {}", unifier.toString(sourceTerm));
                    return fail(c, state);
                }
                if(params.isClosed(source, state)) {
                    return fail(c, state);
                }
                final Scope target;
                if((target = AScope.matcher().match(targetTerm, unifier).orElse(null)) == null) {
                    debug.error("Expected target scope, got {}", unifier.toString(targetTerm));
                    return fail(c, state);
                }
                final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                        state.scopeGraph().addEdge(source, label, target);
                return success(c, state.withScopeGraph(scopeGraph), NO_UPDATED_VARS, NO_NEW_CONSTRAINTS,
                        NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS, fuel);
            }

            @Override public IState.Immutable caseTermId(CAstId c) throws InterruptedException {
                final ITerm term = c.astTerm();
                final ITerm idTerm = c.idTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!(unifier.isGround(term))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(term)));
                }
                final CEqual eq;
                final Optional<Scope> maybeScope = AScope.matcher().match(term, unifier);
                if(maybeScope.isPresent()) {
                    final AScope scope = maybeScope.get();
                    eq = new CEqual(idTerm, scope);
                    return success(c, state, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES,
                            NO_EXISTENTIALS, fuel);
                } else {
                    final Optional<TermIndex> maybeIndex = TermIndex.get(unifier.findTerm(term));
                    if(maybeIndex.isPresent()) {
                        final ITerm indexTerm = TermOrigin.copy(term, maybeIndex.get());
                        eq = new CEqual(idTerm, indexTerm);
                        return success(c, state, NO_UPDATED_VARS, ImmutableList.of(eq), NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        return fail(c, state);
                    }
                }
            }

            @Override public IState.Immutable caseTermProperty(CAstProperty c) throws InterruptedException {
                final ITerm idTerm = c.idTerm();
                final ITerm prop = c.property();
                final ITerm value = c.value();

                final IUniDisunifier unifier = state.unifier();
                if(!(unifier.isGround(idTerm))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(idTerm)));
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
                                return fail(c, state);
                            }
                            property = property.addValue(value);
                            break;
                        }
                        case SET: {
                            if(state.termProperties().containsKey(key)) {
                                return fail(c, state);
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
                    return fail(c, state);
                }
            }

            @Override public IState.Immutable caseTrue(CTrue c) throws InterruptedException {
                return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES, NO_EXISTENTIALS,
                        fuel);
            }

            @Override public IState.Immutable caseTry(CTry c) throws InterruptedException {
                final IDebugContext debug = params.debug();
                try {
                    if(Solver.entails(spec, state, c.constraint(), params::isComplete, new NullDebugContext(),
                            progress.subProgress(1), cancel)) {
                        return success(c, state, NO_UPDATED_VARS, NO_NEW_CONSTRAINTS, NO_NEW_CRITICAL_EDGES,
                                NO_EXISTENTIALS, fuel);
                    } else {
                        return fail(c, state);
                    }
                } catch(Delay e) {
                    debug.debug("Try delayed: {}", e.getMessage());
                    return delay(c, state, e);
                }
            }

            @Override public IState.Immutable caseUser(CUser c) throws InterruptedException {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final LazyDebugContext proxyDebug = new LazyDebugContext(debug);
                final IDebugContext debug = params.debug();

                final List<Rule> rules = spec.rules().getRules(name);
                final List<Tuple2<Rule, ApplyResult>> results =
                        RuleUtil.applyOrderedAll(state.unifier(), rules, args, c, ApplyMode.RELAXED);
                if(results.isEmpty()) {
                    debug.debug("No rule applies");
                    return fail(c, state);
                } else if(results.size() == 1) {
                    final ApplyResult applyResult = results.get(0)._2();
                    proxyDebug.debug("Rule accepted");
                    proxyDebug.commit();
                    if(Solver.INCREMENTAL_CRITICAL_EDGES && applyResult.criticalEdges() == null) {
                        throw new IllegalArgumentException(
                                "Solver only accepts specs with pre-computed critical edges.");
                    }
                    final ICompleteness.Immutable newCriticalEdges =
                            Optional.ofNullable(applyResult.criticalEdges()).orElse(NO_NEW_CRITICAL_EDGES);
                    return success(c, state, NO_UPDATED_VARS, disjoin(applyResult.body()), newCriticalEdges,
                            NO_EXISTENTIALS, fuel);
                } else {
                    final Set<ITermVar> stuckVars = results.stream().flatMap(r -> Streams.stream(r._2().guard()))
                            .flatMap(g -> g.domainSet().stream()).collect(Collectors.toSet());
                    proxyDebug.debug("Rule delayed (multiple conditional matches)");
                    return delay(c, state, Delay.ofVars(stuckVars));
                }
            }

        });

    }

}
