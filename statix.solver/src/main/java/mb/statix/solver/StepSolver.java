package mb.statix.solver;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.relations.IRelation;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidVarsException;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.reference.IncompleteDataException;
import mb.statix.scopegraph.reference.IncompleteEdgeException;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.SolverException.SolverInterrupted;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CFalse;
import mb.statix.solver.constraint.CInequal;
import mb.statix.solver.constraint.CNew;
import mb.statix.solver.constraint.CPathLt;
import mb.statix.solver.constraint.CPathMatch;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.solver.constraint.CTellEdge;
import mb.statix.solver.constraint.CTellRel;
import mb.statix.solver.constraint.CTermId;
import mb.statix.solver.constraint.CTrue;
import mb.statix.solver.constraint.CUser;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.solver.store.BaseConstraintStore;
import mb.statix.spec.Rule;
import mb.statix.spoofax.StatixTerms;

public class StepSolver implements IConstraint.CheckedCases<Optional<ConstraintResult>, SolverException> {

    private State state;
    private final IsComplete isComplete;
    private final ICompleteness completeness;
    private final ConstraintContext params;

    private final IDebugContext debug;
    private final LazyDebugContext proxyDebug;
    private final IDebugContext subDebug;

    public StepSolver(State state, IsComplete _isComplete, IDebugContext debug) {
        this.state = state;
        this.completeness = new IncrementalCompleteness(state.spec());
        this.isComplete = (s, l, st) -> completeness.isComplete(s, l, st.unifier()) && _isComplete.test(s, l, st);
        this.debug = debug;
        this.proxyDebug = new LazyDebugContext(debug);
        this.subDebug = proxyDebug.subContext();
        this.params = new ConstraintContext(this.isComplete, subDebug);
    }

    public SolverResult solve(final Iterable<IConstraint> _constraints) throws InterruptedException {
        debug.info("Solving constraints");

        // set-up
        completeness.addAll(_constraints, state.unifier());
        final IConstraintStore constraints = new BaseConstraintStore(debug);
        constraints.addAll(_constraints);

        // time log
        final Map<Class<? extends IConstraint>, Long> successCount = Maps.newHashMap();
        final Map<Class<? extends IConstraint>, Long> delayCount = Maps.newHashMap();

        // fixed point
        final List<IConstraint> failed = new ArrayList<>();
        final Log delayedLog = new Log();
        boolean progress = true;
        int reductions = 0;
        int delays = 0;
        outer: while(progress) {
            progress = false;
            delayedLog.clear();
            IConstraint constraint;
            while((constraint = constraints.remove()) != null) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if(proxyDebug.isEnabled(Level.Info)) {
                    proxyDebug.info("Solving {}", constraint.toString(Solver.shallowTermFormatter(state.unifier())));
                }
                try {
                    final Optional<ConstraintResult> maybeResult;
                    maybeResult = solve(constraint);
                    addTime(constraint, 1, successCount, debug);
                    progress = true;
                    completeness.remove(constraint, state.unifier());
                    reductions += 1;
                    if(maybeResult.isPresent()) {
                        final ConstraintResult result = maybeResult.get();
                        state = result.state();
                        if(!result.constraints().isEmpty()) {
                            subDebug.info("Simplified to:");
                            for(IConstraint newConstraint : result.constraints()) {
                                if(subDebug.isEnabled(Level.Info)) {
                                    subDebug.info(" * {}", Solver.toString(newConstraint, state.unifier()));
                                }
                                completeness.add(newConstraint, state.unifier());
                                constraints.add(newConstraint);
                            }
                        }
                        completeness.updateAll(result.vars(), state.unifier());
                        constraints.activateFromVars(result.vars(), subDebug);
                        constraints.activateFromEdges(Completeness.criticalEdges(constraint, result.state()), subDebug);
                    } else {
                        subDebug.error("Failed");
                        failed.add(constraint);
                        if(proxyDebug.isRoot()) {
                            Solver.printTrace(constraint, state.unifier(), subDebug);
                        } else {
                            proxyDebug.info("Break early because of errors.");
                            break outer;
                        }
                    }
                    proxyDebug.commit();
                } catch(Delay d) {
                    addTime(constraint, 1, delayCount, debug);
                    subDebug.info("Delayed");
                    delayedLog.absorb(proxyDebug.clear());
                    constraints.delay(constraint, d);
                    delays += 1;
                }
            }
        }

        // invariant: there should be no remaining active constraints
        if(constraints.activeSize() > 0) {
            debug.warn("Expected no remaining active constraints, but got ", constraints.activeSize());
        }

        final Map<IConstraint, Delay> delayed = constraints.delayed();
        delayedLog.flush(debug);
        debug.info("Solved {} constraints ({} delays) with {} failed, and {} remaining constraint(s).", reductions,
                delays, failed.size(), constraints.delayedSize());
        logTimes("success", successCount, debug);
        logTimes("delay", delayCount, debug);

        return SolverResult.of(state, failed, delayed);
    }

    private static void addTime(IConstraint c, long dt, Map<Class<? extends IConstraint>, Long> times,
            IDebugContext debug) {
        if(!debug.isEnabled(Level.Info)) {
            return;
        }
        final Class<? extends IConstraint> key = c.getClass();
        final long t = times.getOrDefault(key, 0L).longValue() + dt;
        times.put(key, t);
    }

    private static void logTimes(String name, Map<Class<? extends IConstraint>, Long> times, IDebugContext debug) {
        debug.info("# ----- {} -----", name);
        for(Map.Entry<Class<? extends IConstraint>, Long> entry : times.entrySet()) {
            debug.info("{} : {}x", entry.getKey().getSimpleName(), entry.getValue());
        }
        debug.info("# ----- {} -----", "-");
    }

    private Optional<ConstraintResult> solve(final IConstraint constraint) throws Delay, InterruptedException {
        try {
            return constraint.matchOrThrow(this);
        } catch(SolverException ex) {
            ex.rethrow();
            throw new IllegalStateException("something should have been thrown");
        }
    }

    @Override public Optional<ConstraintResult> caseEqual(CEqual c) throws SolverException {
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
                return Optional.of(ConstraintResult.ofVars(newState, result.result().varSet()));
            } else {
                if(debug.isEnabled(Level.Info)) {
                    debug.info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
                }
                return Optional.empty();
            }
        } catch(OccursException e) {
            if(debug.isEnabled(Level.Info)) {
                debug.info("Unification failed: {} != {}", unifier.toString(term1), unifier.toString(term2));
            }
            return Optional.empty();
        } catch(RigidVarsException e) {
            throw Delay.ofVars(e.vars());
        }
    }

    @Override public Optional<ConstraintResult> caseFalse(CFalse c) throws SolverException {
        return Optional.empty();
    }

    @Override public Optional<ConstraintResult> caseInequal(CInequal c) throws SolverException {
        final ITerm term1 = c.term1();
        final ITerm term2 = c.term2();

        final IUnifier.Immutable unifier = state.unifier();
        return unifier.areEqual(term1, term2).matchOrThrow(result -> {
            if(result) {
                return Optional.empty();
            } else {
                return Optional.of(ConstraintResult.of(state));
            }
        }, vars -> {
            throw Delay.ofVars(vars);
        });
    }

    @Override public Optional<ConstraintResult> caseNew(CNew c) throws SolverException {
        final List<ITerm> terms = c.terms();

        final List<IConstraint> constraints = Lists.newArrayList();
        State newState = state;
        for(ITerm t : terms) {
            final String base = M.var(ITermVar::getName).match(t).orElse("s");
            Tuple2<Scope, State> ss = newState.freshScope(base);
            constraints.add(new CEqual(t, ss._1(), c));
            newState = ss._2();
        }
        return Optional.of(ConstraintResult.ofConstraints(newState, constraints));
    }

    @Override public Optional<ConstraintResult> casePathLt(CPathLt c) throws SolverException {
        final IRelation.Immutable<ITerm> lt = c.lt();
        final ITerm label1Term = c.label1Term();
        final ITerm label2Term = c.label2Term();

        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(label1Term))) {
            throw Delay.ofVars(unifier.getVars(label1Term));
        }
        if(!(unifier.isGround(label2Term))) {
            throw Delay.ofVars(unifier.getVars(label2Term));
        }
        final ITerm label1 = StatixTerms.label().match(label1Term, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + unifier.toString(label1Term)));
        final ITerm label2 = StatixTerms.label().match(label2Term, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + unifier.toString(label2Term)));
        if(lt.contains(label1, label2)) {
            return Optional.of(ConstraintResult.of(state));
        } else {
            return Optional.empty();
        }
    }

    @Override public Optional<ConstraintResult> casePathMatch(CPathMatch c) throws SolverException {
        final IRegExpMatcher<ITerm> re = c.re();
        final IListTerm labelsTerm = c.labelsTerm();

        final IUnifier unifier = state.unifier();
        // @formatter:off
        return ((IListTerm) unifier.findTerm(labelsTerm)).matchOrThrow(ListTerms.checkedCases(
            cons -> {
                final ITerm labelTerm = cons.getHead();
                if(!unifier.isGround(labelTerm)) {
                    throw Delay.ofVars(unifier.getVars(labelTerm));
                }
                final ITerm label = StatixTerms.label().match(labelTerm, unifier)
                        .orElseThrow(() -> new IllegalArgumentException("Expected label, got " + unifier.toString(labelTerm)));
                final IRegExpMatcher<ITerm> newRe = re.match(label);
                if(newRe.isEmpty()) {
                    return Optional.empty();
                } else {
                    return Optional.of(ConstraintResult.ofConstraints(state, new CPathMatch(newRe, cons.getTail(), c)));
                }
            },
            nil -> {
                if(re.isAccepting()) {
                    return Optional.of(ConstraintResult.of(state));
                } else {
                    return Optional.empty();
                }
            },
            var -> {
                throw Delay.ofVar(var);
            }
        ));
        // @formatter:on
    }

    @Override public Optional<ConstraintResult> caseResolveQuery(CResolveQuery c) throws SolverException {
        final ITerm relation = c.relation();
        final IQueryFilter filter = c.filter();
        final IQueryMin min = c.min();
        final ITerm scopeTerm = c.scopeTerm();
        final ITerm resultTerm = c.resultTerm();

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final Scope scope = AScope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        try {
            final IDebugContext subDebug = new NullDebugContext(params.debug().getDepth() + 1);
            final Predicate2<Scope, ITerm> isComplete = (s, l) -> {
                if(params.isComplete(s, l, state)) {
                    subDebug.info("{} complete in {}", s, l);
                    return true;
                } else {
                    subDebug.info("{} incomplete in {}", s, l);
                    return false;
                }
            };
            // @formatter:off
            final FastNameResolution<Scope, ITerm, ITerm> nameResolution = FastNameResolution.<Scope, ITerm, ITerm>builder()
                        .withLabelWF(filter.getLabelWF(state, params::isComplete, subDebug))
                        .withDataWF(filter.getDataWF(state, params::isComplete, subDebug))
                        .withLabelOrder(min.getLabelOrder(state, params::isComplete, subDebug))
                        .withDataEquiv(min.getDataEquiv(state, params::isComplete, subDebug))
                        .withEdgeComplete(isComplete)
                        .withDataComplete(isComplete)
                        .build(state.scopeGraph(), relation);
            // @formatter:on
            final Set<IResolutionPath<Scope, ITerm, ITerm>> paths = nameResolution.resolve(scope);
            final List<ITerm> pathTerms =
                    paths.stream().map(StatixTerms::explicate).collect(ImmutableList.toImmutableList());
            final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm, c);
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
        } catch(InterruptedException e) {
            throw new SolverInterrupted(e);
        }
    }

    @Override public Optional<ConstraintResult> caseTellEdge(CTellEdge c) throws SolverException {
        final ITerm sourceTerm = c.sourceTerm();
        final ITerm label = c.label();
        final ITerm targetTerm = c.targetTerm();

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(sourceTerm)) {
            throw Delay.ofVars(unifier.getVars(sourceTerm));
        }
        if(!unifier.isGround(targetTerm)) {
            throw Delay.ofVars(unifier.getVars(targetTerm));
        }
        final Scope source = AScope.matcher().match(sourceTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected source scope, got " + unifier.toString(sourceTerm)));
        if(params.isClosed(source, state)) {
            return Optional.empty();
        }
        final Scope target = AScope.matcher().match(targetTerm, unifier).orElseThrow(
                () -> new IllegalArgumentException("Expected target scope, got " + unifier.toString(targetTerm)));
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph = state.scopeGraph().addEdge(source, label, target);
        return Optional.of(ConstraintResult.of(state.withScopeGraph(scopeGraph)));
    }

    @Override public Optional<ConstraintResult> caseTellRel(CTellRel c) throws SolverException {
        final ITerm scopeTerm = c.scopeTerm();
        final ITerm relation = c.relation();
        final ITerm datumTerm = c.datumTerm();

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            throw Delay.ofVars(unifier.getVars(scopeTerm));
        }
        final Scope scope = AScope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));
        if(params.isClosed(scope, state)) {
            return Optional.empty();
        }

        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                state.scopeGraph().addDatum(scope, relation, datumTerm);
        return Optional.of(ConstraintResult.of(state.withScopeGraph(scopeGraph)));
    }

    @Override public Optional<ConstraintResult> caseTermId(CTermId c) throws SolverException {
        final ITerm term = c.term();
        final ITerm idTerm = c.idTerm();

        final IUnifier unifier = state.unifier();
        if(!(unifier.isGround(term))) {
            throw Delay.ofVars(unifier.getVars(term));
        }
        final CEqual eq;
        final Optional<Scope> maybeScope = AScope.matcher().match(term, unifier);
        if(maybeScope.isPresent()) {
            final AScope scope = maybeScope.get();
            eq = new CEqual(idTerm, B.newAppl(StatixTerms.SCOPEID_OP, scope.getArgs()));
        } else {
            final Optional<TermIndex> maybeIndex = TermIndex.get(unifier.findTerm(term));
            if(maybeIndex.isPresent()) {
                final TermIndex index = maybeIndex.get();
                eq = new CEqual(idTerm, B.newAppl(StatixTerms.TERMID_OP, index.getArgs()));
            } else {
                eq = new CEqual(idTerm, B.newAppl(StatixTerms.NOID_OP));
            }
        }
        return Optional.of(ConstraintResult.ofConstraints(state, eq));
    }

    @Override public Optional<ConstraintResult> caseTrue(CTrue c) throws SolverException {
        return Optional.of(ConstraintResult.of(state));
    }

    @Override public Optional<ConstraintResult> caseUser(CUser c) throws SolverException {
        final String name = c.name();
        final List<ITerm> args = c.args();

        final IDebugContext debug = params.debug();
        final List<Rule> rules = Lists.newLinkedList(state.spec().rules().get(name));
        final Log unsuccessfulLog = new Log();
        final Iterator<Rule> it = rules.iterator();
        while(it.hasNext()) {
            if(Thread.interrupted()) {
                throw new SolverInterrupted(new InterruptedException());
            }
            final LazyDebugContext proxyDebug = new LazyDebugContext(debug);
            final Rule rawRule = it.next();
            if(proxyDebug.isEnabled(Level.Info)) {
                proxyDebug.info("Try rule {}", rawRule.toString());
            }
            final State instantiatedState;
            final List<IConstraint> instantiatedBody;
            final Tuple3<State, Set<ITermVar>, List<IConstraint>> appl;
            try {
                if((appl = rawRule.apply(args, state, c).orElse(null)) != null) {
                    instantiatedState = appl._1();
                    instantiatedBody = appl._3();
                } else {
                    proxyDebug.info("Rule rejected (mismatching arguments)");
                    unsuccessfulLog.absorb(proxyDebug.clear());
                    continue;
                }
            } catch(Delay d) {
                proxyDebug.info("Rule delayed (unsolved guard constraint)");
                unsuccessfulLog.absorb(proxyDebug.clear());
                unsuccessfulLog.flush(debug);
                throw d;
            }
            proxyDebug.info("Rule accepted");
            proxyDebug.commit();
            return Optional.of(ConstraintResult.ofConstraints(instantiatedState, instantiatedBody));
        }
        debug.info("No rule applies");
        unsuccessfulLog.flush(debug);
        return Optional.empty();
    }

    @Value.Immutable
    static abstract class AConstraintResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract List<IConstraint> constraints();

        @Value.Parameter public abstract List<ITermVar> vars();

        public static ConstraintResult of(State state) {
            return ConstraintResult.of(state, ImmutableList.of(), ImmutableList.of());
        }

        public static ConstraintResult ofConstraints(State state, IConstraint... constraints) {
            return ofConstraints(state, Arrays.asList(constraints));
        }

        public static ConstraintResult ofConstraints(State state, Iterable<? extends IConstraint> constraints) {
            return ConstraintResult.of(state, ImmutableList.copyOf(constraints), ImmutableList.of());
        }

        public static ConstraintResult ofVars(State state, Iterable<? extends ITermVar> vars) {
            return ConstraintResult.of(state, ImmutableList.of(), ImmutableList.copyOf(vars));
        }

    }

}