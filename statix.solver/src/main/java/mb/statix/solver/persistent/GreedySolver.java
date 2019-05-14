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

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.IUnifier.Immutable;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidVarsException;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.constraint.CConj;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CExists;
import mb.statix.solver.constraint.CFalse;
import mb.statix.solver.constraint.CInequal;
import mb.statix.solver.constraint.CNew;
import mb.statix.solver.constraint.CPathLt;
import mb.statix.solver.constraint.CPathMatch;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.solver.constraint.CTellEdge;
import mb.statix.solver.constraint.CTellRel;
import mb.statix.solver.constraint.CTermId;
import mb.statix.solver.constraint.CTermProperty;
import mb.statix.solver.constraint.CTrue;
import mb.statix.solver.constraint.CUser;
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
            Collection<IConstraint> newConstraints, Map<ITermVar, ITermVar> existentials, int fuel)
            throws InterruptedException {
        if(this.existentials == null) {
            this.existentials = existentials;
        }
        final Immutable unifier = state.unifier();
        completeness.remove(constraint, unifier);
        completeness.updateAll(updatedVars, unifier);
        completeness.addAll(newConstraints, unifier);
        constraints.activateFromVars(updatedVars, debug);
        constraints.activateFromEdges(Completeness.criticalEdges(constraint, state.spec(), state.unifier()), debug);
        final IDebugContext subDebug = debug.subContext();
        if(!newConstraints.isEmpty()) {
            subDebug.info("Simplified to:");
            for(IConstraint newConstraint : newConstraints) {
                if(subDebug.isEnabled(Level.Info)) {
                    subDebug.info(" * {}", Solver.toString(newConstraint, unifier));
                }
            }
        }
        for(IConstraint newConstraint : newConstraints) {
            state = k(state, newConstraint, fuel - 1);
        }
        return state;
    }

    private State delay(IConstraint constraint, State state, Delay delay) {
        constraints.delay(constraint, delay);
        return state;
    }

    private State fail(IConstraint constraint, State state) {
        completeness.remove(constraint, state.unifier());
        failed.add(constraint);
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
                final List<IConstraint> newConstraints =
                        ImmutableList.of(c.left().withCause(c), c.right().withCause(c));
                return success(c, state, ImmutableList.of(), newConstraints, ImmutableMap.of(), fuel);
            }

            @Override public State caseEqual(CEqual c) throws InterruptedException {
                final ITerm term1 = c.term1();
                final ITerm term2 = c.term2();
                IDebugContext debug = params.debug();
                IUnifier.Immutable unifier = state.unifier();
                try {
                    final IUnifier.Immutable.Result<IUnifier.Immutable> result;
                    if((result = unifier.unify(term1, term2, v -> params.isRigid(v, state)).orElse(null)) != null) {
                        if(debug.isEnabled(Level.Info)) {
                            debug.info("Unification succeeded: {}", result.result());
                        }
                        final State newState = state.withUnifier(result.unifier());
                        return success(c, newState, result.result().varSet(), ImmutableList.of(), ImmutableMap.of(),
                                fuel);
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
                } catch(RigidVarsException e) {
                    return delay(c, state, Delay.ofVars(e.vars()));
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
                final IConstraint newConstraint = c.constraint().apply(subst).withCause(c);
                return success(c, newState, ImmutableSet.of(), ImmutableList.of(newConstraint), existentials, fuel);
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
                        return success(c, state, ImmutableList.of(), ImmutableList.of(), ImmutableMap.of(), fuel);
                    }
                }, vars -> {
                    return delay(c, state, Delay.ofVars(vars));
                });
            }

            @Override public State caseNew(CNew c) throws InterruptedException {
                final List<ITerm> terms = c.terms();

                final List<IConstraint> constraints = Lists.newArrayList();
                State newState = state;
                for(ITerm t : terms) {
                    final String base = M.var(ITermVar::getName).match(t).orElse("s");
                    Tuple2<Scope, State> ss = newState.freshScope(base);
                    constraints.add(new CEqual(t, ss._1(), c));
                    newState = ss._2();
                }
                return success(c, newState, ImmutableList.of(), constraints, ImmutableMap.of(), fuel);
            }

            @Override public State casePathLt(CPathLt c) throws InterruptedException {
                final IRelation<ITerm> lt = c.lt();
                final ITerm label1Term = c.label1Term();
                final ITerm label2Term = c.label2Term();

                final IUnifier unifier = state.unifier();
                if(!(unifier.isGround(label1Term))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(label1Term)));
                }
                if(!(unifier.isGround(label2Term))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(label2Term)));
                }
                final ITerm label1 = StatixTerms.label().match(label1Term, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected label, got " + unifier.toString(label1Term)));
                final ITerm label2 = StatixTerms.label().match(label2Term, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected label, got " + unifier.toString(label2Term)));
                if(!lt.contains(label1, label2)) {
                    return fail(c, state);
                } else {
                    return success(c, state, ImmutableList.of(), ImmutableList.of(), ImmutableMap.of(), fuel);
                }
            }

            @Override public State casePathMatch(CPathMatch c) throws InterruptedException {
                final IRegExpMatcher<ITerm> re = c.re();
                final IListTerm labelsTerm = c.labelsTerm();

                final IUnifier unifier = state.unifier();
                // @formatter:off
                return ((IListTerm) unifier.findTerm(labelsTerm)).matchOrThrow(ListTerms.checkedCases(
                    cons -> {
                        final ITerm labelTerm = cons.getHead();
                        if(!unifier.isGround(labelTerm)) {
                            return delay(c, state, Delay.ofVars(unifier.getVars(labelTerm)));
                        }
                        final ITerm label = StatixTerms.label().match(labelTerm, unifier)
                                .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + unifier.toString(labelTerm)));
                        final IRegExpMatcher<ITerm> newRe = re.match(label);
                        if(newRe.isEmpty()) {
                            return fail(c, state);
                        } else {
                            return success(c, state, ImmutableList.of(), ImmutableList.of(new CPathMatch(newRe, cons.getTail(), c)), ImmutableMap.of(), fuel);
                        }
                    },
                    nil -> {
                        if(re.isAccepting()) {
                            return success(c, state, ImmutableList.of(), ImmutableList.of(), ImmutableMap.of(), fuel);
                        } else {
                            return fail(c, state);
                        }
                    },
                    var -> {
                        return delay(c, state, Delay.ofVar(var));
                    }
                ));
                // @formatter:on
            }

            @Override public State caseResolveQuery(CResolveQuery c) throws InterruptedException {
                final ITerm relation = c.relation();
                final IQueryFilter filter = c.filter();
                final IQueryMin min = c.min();
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm resultTerm = c.resultTerm();

                final IUnifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(scopeTerm)));
                }
                final Scope scope = AScope.matcher().match(scopeTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

                try {
                    final Predicate2<Scope, ITerm> isComplete = (s, l) -> {
                        return params.isComplete(s, l, state);
                    };
                    final ConstraintQueries cq = new ConstraintQueries(state, params);
                    // @formatter:off
                    final FastNameResolution<Scope, ITerm, ITerm> nameResolution = FastNameResolution.<Scope, ITerm, ITerm>builder()
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
                    return success(c, state, ImmutableList.of(),
                            ImmutableList.of(new CEqual(B.newList(pathTerms), resultTerm, c)), ImmutableMap.of(), fuel);
                } catch(IncompleteDataException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return delay(c, state, Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.relation())));
                } catch(IncompleteEdgeException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return delay(c, state, Delay.ofCriticalEdge(CriticalEdge.of(e.scope(), e.label())));
                } catch(ResolutionDelayException e) {
                    params.debug().info("Query resolution delayed: {}", e.getMessage());
                    return delay(c, state, e.getCause());
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
                    return delay(c, state, Delay.ofVars(unifier.getVars(sourceTerm)));
                }
                if(!unifier.isGround(targetTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(targetTerm)));
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
                return success(c, state.withScopeGraph(scopeGraph), ImmutableList.of(), ImmutableList.of(),
                        ImmutableMap.of(), fuel);
            }

            @Override public State caseTellRel(CTellRel c) throws InterruptedException {
                final ITerm scopeTerm = c.scopeTerm();
                final ITerm relation = c.relation();
                final ITerm datum = c.datumTerm();

                final IUnifier unifier = state.unifier();
                if(!unifier.isGround(scopeTerm)) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(scopeTerm)));
                }
                final Scope scope = AScope.matcher().match(scopeTerm, unifier).orElseThrow(
                        () -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));
                if(params.isClosed(scope, state)) {
                    return fail(c, state);
                }

                final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                        state.scopeGraph().addDatum(scope, relation, datum);
                return success(c, state.withScopeGraph(scopeGraph), ImmutableList.of(), ImmutableList.of(),
                        ImmutableMap.of(), fuel);
            }

            @Override public State caseTermId(CTermId c) throws InterruptedException {
                final ITerm term = c.term();
                final ITerm idTerm = c.idTerm();

                final IUnifier unifier = state.unifier();
                if(!(unifier.isGround(term))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(term)));
                }
                final CEqual eq;
                final Optional<Scope> maybeScope = AScope.matcher().match(term, unifier);
                if(maybeScope.isPresent()) {
                    final AScope scope = maybeScope.get();
                    final ITerm scopeId1 = B.newAppl(StatixTerms.SCOPEID_OP, scope.getArgs());
                    final ITerm scopeId2 = TermOrigin.get(term).map(o -> o.put(scopeId1)).orElse(scopeId1);
                    eq = new CEqual(idTerm, scopeId2);
                    return success(c, state, ImmutableList.of(), ImmutableList.of(eq), ImmutableMap.of(), fuel);
                } else {
                    final Optional<TermIndex> maybeIndex = TermIndex.get(unifier.findTerm(term));
                    if(maybeIndex.isPresent()) {
                        final TermIndex index = maybeIndex.get();
                        final ITerm termId1 = StatixTerms.explicate(index);
                        final ITerm termId2 = TermOrigin.get(term).map(o -> o.put(termId1)).orElse(termId1);
                        eq = new CEqual(idTerm, termId2);
                        return success(c, state, ImmutableList.of(), ImmutableList.of(eq), ImmutableMap.of(), fuel);
                    } else {
                        return fail(c, state);
                    }
                }
            }

            @Override public State caseTermProperty(CTermProperty c) throws InterruptedException {
                final ITerm idTerm = c.idTerm();
                final ITerm prop = c.property();
                final ITerm value = c.value();

                final IUnifier unifier = state.unifier();
                if(!(unifier.isGround(idTerm))) {
                    return delay(c, state, Delay.ofVars(unifier.getVars(idTerm)));
                }
                final Optional<TermIndex> maybeIndex = StatixTerms.termId().match(idTerm, unifier);
                if(maybeIndex.isPresent()) {
                    final TermIndex index = maybeIndex.get();
                    final IRelation3.Transient<TermIndex, ITerm, ITerm> props = state.termProperties().melt();
                    if(!props.contains(index, prop)) {
                        props.put(index, prop, value);
                        final State newState = state.withTermProperties(props.freeze());
                        return success(c, newState, ImmutableList.of(), ImmutableList.of(), ImmutableMap.of(), fuel);
                    } else {
                        return fail(c, state);
                    }
                } else {
                    return fail(c, state);
                }
            }

            @Override public State caseTrue(CTrue c) throws InterruptedException {
                return success(c, state, ImmutableList.of(), ImmutableList.of(), ImmutableMap.of(), fuel);
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
                        return delay(c, state, d);
                    }
                    proxyDebug.info("Rule accepted");
                    proxyDebug.commit();
                    return success(c, state, ImmutableList.of(), ImmutableList.of(instantiatedBody), ImmutableMap.of(),
                            fuel);
                }
                debug.info("No rule applies");
                unsuccessfulLog.flush(debug);
                return fail(c, state);
            }

        });
    }

}
