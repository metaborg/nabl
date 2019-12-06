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

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.log.Level;

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
import mb.statix.constraints.CTellRel;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.Env;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.NullDebugContext;
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

class GreedySolver {

    private static final int MAX_DEPTH = 32;

    // set-up
    private final Spec spec;
    private final IDebugContext debug;
    private final IConstraintStore constraints;
    private final ICompleteness.Transient completeness;
    private final IState.Immutable initialState;
    private final ConstraintContext params;

    private Map<ITermVar, ITermVar> existentials = null;
    private final List<ITermVar> updatedVars = Lists.newArrayList();
    private final List<CriticalEdge> removedEdges = Lists.newArrayList();
    private final Map<IConstraint, IMessage> failed = Maps.newHashMap();

    public GreedySolver(Spec spec, IState.Immutable state, IConstraint initialConstraint, IsComplete _isComplete,
            IDebugContext debug) {
        this.spec = spec;
        this.initialState = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        constraints.add(initialConstraint);
        this.completeness = Completeness.Transient.of(spec);
        completeness.add(initialConstraint, initialState.unifier());
        final IsComplete isComplete = (s, l, st) -> {
            return completeness.isComplete(s, l, st.unifier()) && _isComplete.test(s, l, st);
        };
        this.params = new ConstraintContext(isComplete, debug);

    }

    public GreedySolver(Spec spec, IState.Immutable state, Iterable<IConstraint> constraints,
            Map<IConstraint, Delay> delays, ICompleteness.Immutable completeness, IDebugContext debug) {
        this.spec = spec;
        this.initialState = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        this.constraints.addAll(constraints);
        this.constraints.delayAll(delays.entrySet());
        this.completeness = completeness.melt();
        // the constraints should already be reflected in completeness
        final IsComplete isComplete = (s, l, st) -> {
            return this.completeness.isComplete(s, l, st.unifier());
        };
        this.params = new ConstraintContext(isComplete, debug);
    }

    public SolverResult solve() throws InterruptedException {
        debug.info("Solving constraints");

        IState.Immutable state = this.initialState;

        IConstraint constraint;
        while((constraint = constraints.remove()) != null) {
            state = step(state, constraint);
        }

        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Expected no remaining active constraints, but got ", constraints.activeSize());
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.info("Solved constraints with {} failed and {} remaining constraint(s).", failed.size(),
                constraints.delayedSize());

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(ImmutableMap.of());
        return SolverResult.of(state, failed, delayed, existentials, updatedVars, removedEdges, completeness.freeze());
    }

    private IState.Immutable step(IState.Immutable state, IConstraint constraint) throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        return k(state, constraint, MAX_DEPTH);
    }


    private IState.Immutable success(IConstraint constraint, IState.Immutable state, Collection<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints, Map<Delay, IConstraint> delayedConstraints,
            Map<ITermVar, ITermVar> existentials, int fuel) throws InterruptedException {
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
        completeness.addAll(newConstraints, unifier); // must come before ICompleteness::remove
        if(subDebug.isEnabled(Level.Info) && !newConstraints.isEmpty()) {
            subDebug.info("Simplified to:");
            for(IConstraint newConstraint : newConstraints) {
                subDebug.info(" * {}", Solver.toString(newConstraint, unifier));
            }
        }

        // add delayed constraints
        delayedConstraints.forEach((d, c) -> constraints.delay(c, d));
        completeness.addAll(delayedConstraints.values(), unifier); // must come before ICompleteness::remove
        if(subDebug.isEnabled(Level.Info) && !delayedConstraints.isEmpty()) {
            subDebug.info("Delayed:");
            for(IConstraint delayedConstraint : delayedConstraints.values()) {
                subDebug.info(" * {}", Solver.toString(delayedConstraint, unifier));
            }
        }

        // remove current constraint (duplicated in ::fail)
        final Set<CriticalEdge> removedEdges = completeness.remove(constraint, unifier);
        constraints.activateFromEdges(removedEdges, debug);
        this.removedEdges.addAll(removedEdges);

        // continue on new constraints
        for(IConstraint newConstraint : newConstraints) {
            state = k(state, newConstraint, fuel - 1);
        }

        return state;
    }

    private IState.Immutable success(IConstraint c, IState.Immutable newState, int fuel) throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), ImmutableList.of(), ImmutableMap.of(), ImmutableMap.of(), fuel);
    }

    private IState.Immutable successNew(IConstraint c, IState.Immutable newState,
            Collection<IConstraint> newConstraints, int fuel) throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), newConstraints, ImmutableMap.of(), ImmutableMap.of(), fuel);
    }

    private IState.Immutable successDelay(IConstraint c, IState.Immutable newState, Delay delay, int fuel)
            throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), ImmutableList.of(), ImmutableMap.of(delay, c), ImmutableMap.of(),
                fuel);
    }

    private IState.Immutable fail(IConstraint constraint, IState.Immutable state) {
        failed.put(constraint, MessageUtil.findClosestMessage(constraint));
        // remove current constraint (duplicated in ::success)
        final Set<CriticalEdge> removedEdges = completeness.remove(constraint, state.unifier());
        constraints.activateFromEdges(removedEdges, debug);
        this.removedEdges.addAll(removedEdges);
        return state;
    }

    private IState.Immutable queue(IConstraint constraint, IState.Immutable state) {
        constraints.add(constraint);
        return state;
    }

    private IState.Immutable k(IState.Immutable state, IConstraint constraint, int fuel) throws InterruptedException {
        // stop if thread is interrupted
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }

        // stop recursion if we run out of fuel
        if(fuel <= 0) {
            return queue(constraint, state);
        }

        if(debug.isEnabled(Level.Info)) {
            debug.info("Solving {}", constraint.toString(Solver.shallowTermFormatter(state.unifier())));
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
                            return fail(c, state);
                        }
                    }
                } catch(Delay d) {
                    return successDelay(c, state, d, fuel);
                }
            }

            @Override public IState.Immutable caseConj(CConj c) throws InterruptedException {
                final List<IConstraint> newConstraints = disjoin(c);
                return successNew(c, state, newConstraints, fuel);
            }

            @Override public IState.Immutable caseEqual(CEqual c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IDebugContext debug = params.debug();
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
                        return fail(c, state);
                    }
                } catch(OccursException e) {
                    if(debug.isEnabled(Level.Info)) {
                        debug.info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
                    }
                    return fail(c, state);
                }
            }

            @Override public IState.Immutable caseExists(CExists c) throws InterruptedException {
                final ImmutableMap.Builder<ITermVar, ITermVar> existentialsBuilder = ImmutableMap.builder();
                IState.Immutable newState = state;
                for(ITermVar var : c.vars()) {
                    final Tuple2<ITermVar, IState.Immutable> varAndState = newState.freshVar(var.getName());
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

            @Override public IState.Immutable caseFalse(CFalse c) {
                return fail(c, state);
            }

            @Override public IState.Immutable caseInequal(CInequal c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IDebugContext debug = params.debug();
                final IUniDisunifier.Immutable unifier = state.unifier();
                final IUniDisunifier.Result<Optional<Diseq>> result;
                if((result = unifier.disunify(c.universals(), term1, term2).orElse(null)) != null) {
                    if(debug.isEnabled(Level.Info)) {
                        debug.info("Disunification succeeded: {}", result);
                    }
                    final IState.Immutable newState = state.withUnifier(result.unifier());
                    final Set<ITermVar> updatedVars =
                            result.result().<Set<ITermVar>>map(Diseq::varSet).orElse(ImmutableSet.of());
                    return success(c, newState, updatedVars, ImmutableList.of(), ImmutableMap.of(), ImmutableMap.of(),
                            fuel);
                } else {
                    if(debug.isEnabled(Level.Info)) {
                        debug.info("Disunification failed");
                    }
                    return fail(c, state);
                }
            }

            @Override public IState.Immutable caseNew(CNew c) throws InterruptedException {
                final List<ITerm> terms = c.terms();

                final List<IConstraint> newConstraints = Lists.newArrayList();
                IState.Immutable newState = state;
                for(ITerm t : terms) {
                    final String base = M.var(ITermVar::getName).match(t).orElse("s");
                    Tuple2<Scope, IState.Immutable> ss = newState.freshScope(base);
                    newConstraints.add(new CEqual(t, ss._1(), c));
                    newState = ss._2();
                }
                return success(c, newState, ImmutableList.of(), newConstraints, ImmutableMap.of(), ImmutableMap.of(),
                        fuel);
            }

            @Override public IState.Immutable caseResolveQuery(CResolveQuery c) throws InterruptedException {
                final ITerm relation = c.relation();
                final IQueryFilter filter = c.filter();
                final IQueryMin min = c.min();
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm resultTerm = c.resultTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(scopeTerm)), fuel);
                }
                final Scope scope = AScope.matcher().match(scopeTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

                try {
                    final Predicate2<Scope, ITerm> isComplete = (s, l) -> {
                        return params.isComplete(s, l, state);
                    };
                    final ConstraintQueries cq = new ConstraintQueries(spec, state, params);
                    // @formatter:off
                    final INameResolution<Scope, ITerm, ITerm> nameResolution = Solver.nameResolutionBuilder()
                                .withLabelWF(cq.getLabelWF(filter.getLabelWF()))
                                .withDataWF(cq.getDataWF(filter.getDataWF()))
                                .withLabelOrder(cq.getLabelOrder(min.getLabelOrder()))
                                .withDataEquiv(cq.getDataEquiv(min.getDataEquiv()))
                                .withEdgeComplete(isComplete)
                                .withDataComplete(isComplete)
                                .build(state.scopeGraph(), relation);
                    // @formatter:on
                    final Env<Scope, ITerm, ITerm> paths = nameResolution.resolve(scope);
                    final List<ITerm> pathTerms =
                            Streams.stream(paths).map(StatixTerms::explicate).collect(ImmutableList.toImmutableList());
                    final IConstraint C = new CEqual(resultTerm, B.newList(pathTerms), c);
                    return successNew(c, state, ImmutableList.of(C), fuel);
                } catch(IncompleteDataException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return successDelay(c, state, Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation())), fuel);
                } catch(IncompleteEdgeException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return successDelay(c, state, Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label())), fuel);
                } catch(ResolutionDelayException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return successDelay(c, state, e.getCause(), fuel);
                } catch(ResolutionException e) {
                    params.debug().info("Query resolution failed: {}", e.getMessage());
                    return fail(c, state);
                }
            }

            @Override public IState.Immutable caseTellEdge(CTellEdge c) throws InterruptedException {
                final ITerm sourceTerm = c.sourceTerm();
                final ITerm label = c.label();
                final ITerm targetTerm = c.targetTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(sourceTerm)) {
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(sourceTerm)), fuel);
                }
                if(!unifier.isGround(targetTerm)) {
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(targetTerm)), fuel);
                }
                final Scope source =
                        AScope.matcher().match(sourceTerm, unifier).orElseThrow(() -> new IllegalArgumentException(
                                "Expected source scope, got " + unifier.toString(sourceTerm)));
                if(params.isClosed(source, state)) {
                    return fail(c, state);
                }
                final Scope target =
                        AScope.matcher().match(targetTerm, unifier).orElseThrow(() -> new IllegalArgumentException(
                                "Expected target scope, got " + unifier.toString(targetTerm)));
                final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                        state.scopeGraph().addEdge(source, label, target);
                return success(c, state.withScopeGraph(scopeGraph), fuel);
            }

            @Override public IState.Immutable caseTellRel(CTellRel c) throws InterruptedException {
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm relation = c.relation();
                final ITerm datum = c.datumTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(scopeTerm)), fuel);
                }
                final Scope scope = AScope.matcher().match(scopeTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));
                if(params.isClosed(scope, state)) {
                    return fail(c, state);
                }

                final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                        state.scopeGraph().addDatum(scope, relation, datum);
                return success(c, state.withScopeGraph(scopeGraph), fuel);
            }

            @Override public IState.Immutable caseTermId(CAstId c) throws InterruptedException {
                final ITerm term = c.astTerm();
                final ITerm idTerm = c.idTerm();

                final IUniDisunifier unifier = state.unifier();
                if(!(unifier.isGround(term))) {
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(term)), fuel);
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
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(idTerm)), fuel);
                }
                final Optional<TermIndex> maybeIndex = TermIndex.matcher().match(idTerm, unifier);
                if(maybeIndex.isPresent()) {
                    final TermIndex index = maybeIndex.get();
                    if(!state.termProperties().contains(index, prop)) {
                        final IState.Immutable newState =
                                state.withTermProperties(state.termProperties().put(index, prop, value));
                        return success(c, newState, fuel);
                    } else {
                        return fail(c, state);
                    }
                } else {
                    return fail(c, state);
                }
            }

            @Override public IState.Immutable caseTrue(CTrue c) throws InterruptedException {
                return success(c, state, fuel);
            }

            @Override public IState.Immutable caseTry(CTry c) throws InterruptedException {
                try {
                    if(Solver.entails(spec, state, c.constraint(), params::isComplete, new NullDebugContext())) {
                        return success(c, state, fuel);
                    } else {
                        return fail(c, state);
                    }
                } catch(Delay e) {
                    params.debug().info("Try delayed: {}", e.getMessage());
                    return successDelay(c, state, e, fuel);
                }
            }

            @Override public IState.Immutable caseUser(CUser c) throws InterruptedException {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final LazyDebugContext proxyDebug = new LazyDebugContext(debug);
                final IDebugContext debug = params.debug();

                final List<Rule> rules = spec.rules().get(name);
                final List<Tuple2<Rule, ApplyResult>> results = RuleUtil.applyAll(state, rules, args, c);
                if(results.isEmpty()) {
                    debug.info("No rule applies");
                    return fail(c, state);
                } else if(results.size() == 1) {
                    final ApplyResult applyResult = results.get(0)._2();
                    proxyDebug.info("Rule accepted");
                    proxyDebug.commit();
                    return success(c, applyResult.state(), applyResult.updatedVars(), disjoin(applyResult.body()),
                            ImmutableMap.of(), ImmutableMap.of(), fuel);
                } else {
                    final Set<ITermVar> stuckVars = results.stream().flatMap(r -> Streams.stream(r._2().guard()))
                            .flatMap(g -> g.varSet().stream()).collect(Collectors.toSet());
                    proxyDebug.info("Rule delayed (multiple conditional matches)");
                    return successDelay(c, state, Delay.ofVars(stuckVars), fuel);
                }
            }

        });

    }

}
