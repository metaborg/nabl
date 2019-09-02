package mb.statix.solver.persistent;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable.Result;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
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
import mb.statix.constraints.CUser;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.persistent.query.ConstraintQueries;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.solver.store.BaseConstraintStore;
import mb.statix.spec.Rule;
import mb.statix.spoofax.StatixTerms;

public class GreedySolver {

    private static final int MAX_DEPTH = 32;

    // set-up
    private final IDebugContext debug;
    private final IConstraintStore constraints;
    private final ICompleteness completeness;
    private final State initialState;
    private final ConstraintContext params;

    private Map<ITermVar, ITermVar> existentials = null;
    private final List<IConstraint> failed = new ArrayList<>();

    public GreedySolver(State state, IsComplete _isComplete, IDebugContext debug) {
        this.initialState = state;
        this.debug = debug;
        this.constraints = new BaseConstraintStore(debug);
        this.completeness = new IncrementalCompleteness(state.spec());
        final IsComplete isComplete = (s, l, st) -> {
            return completeness.isComplete(s, l, st.unifier()) && _isComplete.test(s, l, st);
        };
        this.params = new ConstraintContext(isComplete, debug);
    }

    public SolverResult solve(IConstraint initialConstraint) throws InterruptedException {
        debug.info("Solving constraints");

        State state = this.initialState;

        completeness.add(initialConstraint, state.unifier());
        state = step(state, initialConstraint);

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
        return SolverResult.of(state, failed, delayed, existentials);
    }

    private State step(State state, IConstraint constraint) throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
        return k(state, constraint, MAX_DEPTH);
    }


    private State success(IConstraint constraint, State state, Collection<ITermVar> updatedVars,
            Collection<IConstraint> newConstraints, Map<Delay, IConstraint> delayedConstraints,
            Map<ITermVar, ITermVar> existentials, int fuel) throws InterruptedException {
        final IDebugContext subDebug = debug.subContext();
        if(this.existentials == null) {
            this.existentials = existentials;
        }
        final IUnifier.Immutable unifier = state.unifier();

        // updates from unified variables
        completeness.updateAll(updatedVars, unifier);
        constraints.activateFromVars(updatedVars, debug);

        // add new constraints
        // no constraints::addAll, instead recurse immediately below
        completeness.addAll(newConstraints, unifier); // must come before ICompleteness::remove
        if(!newConstraints.isEmpty()) {
            subDebug.info("Simplified to:");
            for(IConstraint newConstraint : newConstraints) {
                if(subDebug.isEnabled(Level.Info)) {
                    subDebug.info(" * {}", Solver.toString(newConstraint, unifier));
                }
            }
        }

        // add delayed constraints
        delayedConstraints.forEach((d, c) -> constraints.delay(c, d));
        completeness.addAll(delayedConstraints.values(), unifier); // must come before ICompleteness::remove
        if(!delayedConstraints.isEmpty()) {
            subDebug.info("Delayed:");
            for(IConstraint delayedConstraint : delayedConstraints.values()) {
                if(subDebug.isEnabled(Level.Info)) {
                    subDebug.info(" * {}", Solver.toString(delayedConstraint, state.unifier()));
                }
            }
        }

        // remove current constraint (duplicated in ::fail)
        final Set<CriticalEdge> removedEdges = completeness.remove(constraint, unifier);
        constraints.activateFromEdges(removedEdges, debug);

        // continue on new constraints
        for(IConstraint newConstraint : newConstraints) {
            state = k(state, newConstraint, fuel - 1);
        }

        return state;
    }

    private State success(IConstraint c, State newState, int fuel) throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), ImmutableList.of(), ImmutableMap.of(), ImmutableMap.of(), fuel);
    }

    private State successNew(IConstraint c, State newState, Collection<IConstraint> newConstraints, int fuel)
            throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), newConstraints, ImmutableMap.of(), ImmutableMap.of(), fuel);
    }

    private State successDelay(IConstraint c, State newState, Delay delay, int fuel) throws InterruptedException {
        return success(c, newState, ImmutableSet.of(), ImmutableList.of(), ImmutableMap.of(delay, c), ImmutableMap.of(),
                fuel);
    }

    private State fail(IConstraint constraint, State state) {
        failed.add(constraint);
        // remove current constraint (duplicated in ::success)
        final Set<CriticalEdge> removedEdges = completeness.remove(constraint, state.unifier());
        constraints.activateFromEdges(removedEdges, debug);
        return state;
    }

    private State queue(IConstraint constraint, State state) {
        constraints.add(constraint);
        return state;
    }

    private State k(State state, IConstraint constraint, int fuel) throws InterruptedException {
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
        return constraint.matchOrThrow(new IConstraint.CheckedCases<State, InterruptedException>() {

            @Override public State caseConj(CConj c) throws InterruptedException {
                // @formatter:off
                final List<IConstraint> newConstraints = ImmutableList.of(
                        c.left().withCause(c.cause().orElse(null)),
                        c.right().withCause(c.cause().orElse(null)));
                // @formatter:on
                return successNew(c, state, newConstraints, fuel);
            }

            @Override public State caseEqual(CEqual c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IDebugContext debug = params.debug();
                IUnifier.Immutable unifier = state.unifier();
                try {
                    final Result<Tuple2<IUnifier.Immutable, Collection<Tuple2<ITermVar, ITerm>>>> result;
                    if((result = unifier.unify(term1, term2, v -> params.isRigid(v, state)).orElse(null)) != null) {
                        if(debug.isEnabled(Level.Info)) {
                            debug.info("Unification succeeded: {}", result.result());
                        }
                        final State newState = state.withUnifier(result.unifier());
                        final Set<ITermVar> updatedVars = result.result()._1().varSet();
                        final Map<Delay, IConstraint> delayedConstraints =
                                Solver.rigidsToDelays(result.result()._2(), c.cause());
                        return success(c, newState, updatedVars, ImmutableList.of(), delayedConstraints,
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

            @Override public State caseExists(CExists c) throws InterruptedException {
                final ImmutableMap.Builder<ITermVar, ITermVar> existentialsBuilder = ImmutableMap.builder();
                State newState = state;
                for(ITermVar var : c.vars()) {
                    final Tuple2<ITermVar, State> varAndState = newState.freshVar(var.getName());
                    final ITermVar freshVar = varAndState._1();
                    newState = varAndState._2();
                    existentialsBuilder.put(var, freshVar);
                }
                final Map<ITermVar, ITermVar> existentials = existentialsBuilder.build();
                final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(existentials);
                final IConstraint newConstraint = c.constraint().apply(subst).withCause(c.cause().orElse(null));
                return success(c, newState, ImmutableSet.of(), ImmutableList.of(newConstraint), ImmutableMap.of(),
                        existentials, fuel);
            }

            @Override public State caseFalse(CFalse c) {
                return fail(c, state);
            }

            @Override public State caseInequal(CInequal c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();

                final IUnifier unifier = state.unifier();
                return unifier.areEqual(term1, term2).matchOrThrow(result -> {
                    if(result) {
                        return fail(c, state);
                    } else {
                        return success(c, state, fuel);
                    }
                }, vars -> {
                    return successDelay(c, state, Delay.ofVars(vars), fuel);
                });
            }

            @Override public State caseNew(CNew c) throws InterruptedException {
                final List<ITerm> terms = c.terms();

                final List<IConstraint> newConstraints = Lists.newArrayList();
                State newState = state;
                for(ITerm t : terms) {
                    final String base = M.var(ITermVar::getName).match(t).orElse("s");
                    Tuple2<Scope, State> ss = newState.freshScope(base);
                    newConstraints.add(new CEqual(t, ss._1(), c));
                    newState = ss._2();
                }
                return success(c, newState, ImmutableList.of(), newConstraints, ImmutableMap.of(), ImmutableMap.of(),
                        fuel);
            }

            @Override public State caseResolveQuery(CResolveQuery c) throws InterruptedException {
                final ITerm relation = c.relation();
                final IQueryFilter filter = c.filter();
                final IQueryMin min = c.min();
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm resultTerm = c.resultTerm();

                final IUnifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(scopeTerm)), fuel);
                }
                final Scope scope = AScope.matcher().match(scopeTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

                try {
                    final Predicate2<Scope, ITerm> isComplete = (s, l) -> {
                        return params.isComplete(s, l, state);
                    };
                    final ConstraintQueries cq = new ConstraintQueries(state, params);
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
                    final Set<IResolutionPath<Scope, ITerm, ITerm>> paths = nameResolution.resolve(scope);
                    final List<ITerm> pathTerms =
                            paths.stream().map(StatixTerms::explicate).collect(ImmutableList.toImmutableList());
                    final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm, c);
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

            @Override public State caseTellEdge(CTellEdge c) throws InterruptedException {
                final ITerm sourceTerm = c.sourceTerm();
                final ITerm label = c.label();
                final ITerm targetTerm = c.targetTerm();

                final IUnifier unifier = state.unifier();
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

            @Override public State caseTellRel(CTellRel c) throws InterruptedException {
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm relation = c.relation();
                final ITerm datum = c.datumTerm();

                final IUnifier unifier = state.unifier();
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

            @Override public State caseTermId(CAstId c) throws InterruptedException {
                final ITerm term = c.astTerm();
                final ITerm idTerm = c.idTerm();

                final IUnifier unifier = state.unifier();
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

            @Override public State caseTermProperty(CAstProperty c) throws InterruptedException {
                final ITerm idTerm = c.idTerm();
                final ITerm prop = c.property();
                final ITerm value = c.value();

                final IUnifier unifier = state.unifier();
                if(!(unifier.isGround(idTerm))) {
                    return successDelay(c, state, Delay.ofVars(unifier.getVars(idTerm)), fuel);
                }
                final Optional<TermIndex> maybeIndex = TermIndex.matcher().match(idTerm, unifier);
                if(maybeIndex.isPresent()) {
                    final TermIndex index = maybeIndex.get();
                    final Tuple2<TermIndex, ITerm> key = ImmutableTuple2.of(index, prop);
                    if(!state.termProperties().containsKey(key)) {
                        final ImmutableMap.Builder<Tuple2<TermIndex, ITerm>, ITerm> props = ImmutableMap.builder();
                        props.putAll(state.termProperties());
                        props.put(key, value);
                        final State newState = state.withTermProperties(props.build());
                        return success(c, newState, fuel);
                    } else {
                        return fail(c, state);
                    }
                } else {
                    return fail(c, state);
                }
            }

            @Override public State caseTrue(CTrue c) throws InterruptedException {
                return success(c, state, fuel);
            }

            @Override public State caseUser(CUser c) throws InterruptedException {
                final String name = c.name();
                final List<ITerm> args = c.args();

                final IDebugContext debug = params.debug();
                final List<Rule> rules = Lists.newLinkedList(state.spec().rules().get(name));
                final Log unsuccessfulLog = new Log();
                final Iterator<Rule> it = rules.iterator();
                while(it.hasNext()) {
                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    final LazyDebugContext proxyDebug = new LazyDebugContext(debug);
                    final Rule rawRule = it.next();
                    if(proxyDebug.isEnabled(Level.Info)) {
                        proxyDebug.info("Try rule {}", rawRule.toString());
                    }
                    final IConstraint instantiatedBody;
                    try {
                        if((instantiatedBody = rawRule.apply(args, state.unifier(), c).orElse(null)) == null) {
                            proxyDebug.info("Rule rejected (mismatching arguments)");
                            unsuccessfulLog.absorb(proxyDebug.clear());
                            continue;
                        }
                    } catch(Delay d) {
                        proxyDebug.info("Rule delayed (unsolved guard constraint)");
                        unsuccessfulLog.absorb(proxyDebug.clear());
                        unsuccessfulLog.flush(debug);
                        return successDelay(c, state, d, fuel);
                    }
                    proxyDebug.info("Rule accepted");
                    proxyDebug.commit();
                    return successNew(c, state, ImmutableList.of(instantiatedBody), fuel);
                }
                debug.info("No rule applies");
                unsuccessfulLog.flush(debug);
                return fail(c, state);
            }

        });
    }

}
