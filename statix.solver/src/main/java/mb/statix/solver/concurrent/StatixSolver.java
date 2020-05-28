package mb.statix.solver.concurrent;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.constraints.Constraints.disjoin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
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
import mb.statix.solver.store.BaseConstraintStore;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

public class StatixSolver {

    private static final int MAX_DEPTH = 32;

    private final Spec spec;
    private final IConstraintStore constraints;
    private final IDebugContext debug;
    private final IScopeGraph<Scope, ITerm, ITerm> scopeGraph;

    private IState.Immutable state;
    private Map<ITermVar, ITermVar> existentials = null;
    private final List<ITermVar> updatedVars = Lists.newArrayList();
    private final Map<IConstraint, IMessage> failed = Maps.newHashMap();

    private final CompletableFuture<SolverResult> result = new CompletableFuture<>();
    private int pending = 0;

    public StatixSolver(Spec spec, IConstraint constraint, IDebugContext debug,
            IScopeGraph<Scope, ITerm, ITerm> scopeGraph) {
        this.spec = spec;
        this.constraints = new BaseConstraintStore(debug);
        this.constraints.add(constraint);
        this.debug = debug;
        this.scopeGraph = scopeGraph;
        this.state = mb.statix.solver.persistent.State.of(spec);
    }

    public CompletableFuture<SolverResult> solve() {
        try {
            run();
        } catch(Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private void run() throws InterruptedException {
        if(result.isDone()) {
            throw new IllegalStateException("Cannot run when already completed.");
        }
        debug.info("Solving constraints");

        IConstraint constraint;
        while((constraint = constraints.remove()) != null) {
            state = step(state, constraint);
        }

        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            throw new IllegalStateException(
                    "Expected no remaining active constraints, but got " + constraints.activeSize());
        }

        if(pending == 0) {
            result.complete(finish());
        }
    }

    public SolverResult finish() {
        final Map<IConstraint, Delay> delayed = constraints.delayed();
        debug.info("Solved constraints with {} failed and {} remaining constraint(s).", failed.size(),
                constraints.delayedSize());
        for(Delay delayedConstraint : delayed.values()) {
            debug.info(" * {}", delayedConstraint.toString());
        }

        final Map<ITermVar, ITermVar> existentials = Optional.ofNullable(this.existentials).orElse(ImmutableMap.of());
        final java.util.Set<CriticalEdge> removedEdges = ImmutableSet.of();
        final ICompleteness.Immutable completeness = Completeness.Immutable.of(spec);
        final SolverResult result =
                SolverResult.of(state, failed, delayed, existentials, updatedVars, removedEdges, completeness);
        return result;
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
        constraints.activateFromVars(updatedVars, debug);
        this.updatedVars.addAll(updatedVars);

        // add new constraints
        // no constraints::addAll, instead recurse immediately below
        if(subDebug.isEnabled(Level.Info) && !newConstraints.isEmpty()) {
            subDebug.info("Simplified to:");
            for(IConstraint newConstraint : newConstraints) {
                subDebug.info(" * {}", Solver.toString(newConstraint, unifier));
            }
        }

        // add delayed constraints
        delayedConstraints.forEach((d, c) -> constraints.delay(c, d));
        if(subDebug.isEnabled(Level.Info) && !delayedConstraints.isEmpty()) {
            subDebug.info("Delayed:");
            for(IConstraint delayedConstraint : delayedConstraints.values()) {
                subDebug.info(" * {}", Solver.toString(delayedConstraint, unifier));
            }
        }

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

            @Override public IState.Immutable caseFalse(CFalse c) {
                return fail(c, state);
            }

            @Override public IState.Immutable caseInequal(CInequal c) throws InterruptedException {
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
                return fail(c, state);
                /*
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
                
                return success(c, newState, ImmutableList.of(), ImmutableList.of(eq), ImmutableMap.of(),
                        ImmutableMap.of(), fuel);
                */
            }

            @Override public IState.Immutable caseResolveQuery(CResolveQuery c) throws InterruptedException {
                return fail(c, state);
                /*
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
                    final ConstraintQueries cq = new ConstraintQueries(spec, state, params);
                    // @formatter:off
                    final INameResolution<Scope, ITerm, ITerm> nameResolution = Solver.nameResolutionBuilder()
                                .withLabelWF(cq.getLabelWF(filter.getLabelWF()))
                                .withDataWF(cq.getDataWF(filter.getDataWF()))
                                .withLabelOrder(cq.getLabelOrder(min.getLabelOrder()))
                                .withDataEquiv(cq.getDataEquiv(min.getDataEquiv()))
                                .withIsComplete((s, l) -> params.isComplete(s, l, state))
                                .build(state.scopeGraph());
                    // @formatter:on
                    final Env<Scope, ITerm, ITerm> paths = nameResolution.resolve(scope);
                    final List<ITerm> pathTerms =
                            Streams.stream(paths).map(p -> StatixTerms.explicate(p, spec.dataLabels()))
                                    .collect(ImmutableList.toImmutableList());
                    final IConstraint C = new CEqual(resultTerm, B.newList(pathTerms), c);
                    return successNew(c, state, ImmutableList.of(C), fuel);
                } catch(IncompleteException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return successDelay(c, state, Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label())), fuel);
                } catch(ResolutionDelayException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return successDelay(c, state, e.getCause(), fuel);
                } catch(ResolutionException e) {
                    params.debug().info("Query resolution failed: {}", e.getMessage());
                    return fail(c, state);
                }
                */
            }

            @Override public IState.Immutable caseTellEdge(CTellEdge c) throws InterruptedException {
                return fail(c, state);
                /*
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
                */
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
                    return success(c, newState, fuel);
                } else {
                    return fail(c, state);
                }
            }

            @Override public IState.Immutable caseTrue(CTrue c) throws InterruptedException {
                return success(c, state, fuel);
            }

            @Override public IState.Immutable caseTry(CTry c) throws InterruptedException {
                return fail(c, state);
                /*
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
                */
            }

            @Override public IState.Immutable caseUser(CUser c) throws InterruptedException {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final LazyDebugContext proxyDebug = new LazyDebugContext(debug);

                final List<Rule> rules = spec.rules().getRules(name);
                final List<Tuple2<Rule, ApplyResult>> results = RuleUtil.applyOrderedAll(state, rules, args, c);
                if(results.isEmpty()) {
                    debug.info("No rule applies");
                    return fail(c, state);
                } else if(results.size() == 1) {
                    final ApplyResult applyResult = results.get(0)._2();
                    proxyDebug.info("Rule accepted");
                    proxyDebug.info("| Implied equalities: {}", applyResult.diff());
                    proxyDebug.commit();
                    return success(c, applyResult.state(), applyResult.diff().varSet(), disjoin(applyResult.body()),
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

    @Override public String toString() {
        return "StatixSolver";
    }

}