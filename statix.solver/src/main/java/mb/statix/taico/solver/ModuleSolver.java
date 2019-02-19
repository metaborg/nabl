package mb.statix.taico.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.log.Level;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Completeness;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.ConstraintResult;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LazyDebugContext;
import mb.statix.solver.log.Log;
import mb.statix.solver.store.BaseConstraintStore;

public class ModuleSolver implements Callable<SolverResult> {
    private State initState;
    private Iterable<IConstraint> initConstraints;
    private Completeness initCompleteness;
    private IDebugContext initDebug;
    private Predicate1<ITermVar> initIsRigid;
    private Predicate1<ITerm> initIsClosed;
    

    public ModuleSolver(State state, Iterable<IConstraint> constraints, Completeness completeness, IDebugContext debug) {
        this(state, constraints, completeness, v -> false, s -> false, debug);
    }
    
    public ModuleSolver(State state, Iterable<IConstraint> constraints, Completeness completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed, IDebugContext debug) {
        this.initState = state;
        this.initConstraints = constraints;
        this.initCompleteness = completeness;
        this.initDebug = debug;
        this.initIsRigid = isRigid;
        this.initIsClosed = isClosed;
    }
    
    public ModuleSolver() {
    }
    
    @Override
    public SolverResult call() throws InterruptedException {
        return solve();
    }
    
    public SolverResult solve() throws InterruptedException {
        return solve(initState, initConstraints, initCompleteness, initIsRigid, initIsClosed, initDebug);
    }

    protected SolverResult solve(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final IDebugContext debug) throws InterruptedException {
        return solve(state, constraints, completeness, v -> false, s -> false, debug);
    }

    protected SolverResult solve(final State _state, final Iterable<IConstraint> _constraints,
            final Completeness _completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed,
            final IDebugContext debug) throws InterruptedException {
        debug.info("Solving constraints");
        
        //TODO Solve and store state continuously. If no more progress is being made then throw delay exception
        //TODO the parent solver will then handle the delay
        final LazyDebugContext proxyDebug = new LazyDebugContext(debug);

        // set-up
        final IConstraintStore constraints = new BaseConstraintStore(_constraints, debug);
        State state = _state;
        Completeness completeness = _completeness;
        completeness = completeness.addAll(_constraints);

        // time log
        final Map<Class<? extends IConstraint>, Long> successCount = new HashMap<>();
        final Map<Class<? extends IConstraint>, Long> delayCount = new HashMap<>();

        // fixed point
        final Set<IConstraint> failed = new HashSet<>();
        final Log delayedLog = new Log();
        boolean progress = true;
        int reductions = 0;
        int delays = 0;
        outer: while(progress) {
            progress = false;
            constraints.activateStray();
            delayedLog.clear();
            for(IConstraintStore.Entry entry : constraints.active(debug)) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                IDebugContext subDebug = proxyDebug.subContext();
                final IConstraint constraint = entry.constraint();
                if(proxyDebug.isEnabled(Level.Info)) {
                    proxyDebug.info("Solving {}", constraint.toString(ModuleSolver.shallowTermFormatter(state.unifier())));
                }
                try {
                    final Optional<ConstraintResult> maybeResult;
                    maybeResult =
                            constraint.solve(state, new ConstraintContext(completeness, isRigid, isClosed, subDebug));
                    addTime(constraint, 1, successCount, debug);
                    progress = true;
                    entry.remove();
                    completeness = completeness.remove(constraint);
                    reductions += 1;
                    if(maybeResult.isPresent()) {
                        final ConstraintResult result = maybeResult.get();
                        state = result.state();
                        if(!result.constraints().isEmpty()) {
                            final List<IConstraint> newConstaints = result.constraints().stream()
                                    .map(c -> c.withCause(constraint)).collect(Collectors.toList());
                            if(subDebug.isEnabled(Level.Info)) {
                                subDebug.info("Simplified to {}", toString(newConstaints, state.unifier()));
                            }
                            constraints.addAll(newConstaints);
                            completeness = completeness.addAll(newConstaints);
                        }
                        constraints.activateFromVars(result.vars(), subDebug);
                        constraints.activateFromEdges(Completeness.criticalEdges(constraint, result.state()), subDebug);
                    } else {
                        subDebug.error("Failed");
                        failed.add(constraint);
                        if(proxyDebug.isRoot()) {
                            printTrace(constraint, state.unifier(), subDebug);
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
                    entry.delay(d);
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

        return SolverResult.of(state, completeness, failed, delayed);
    }

    private void addTime(IConstraint c, long dt, Map<Class<? extends IConstraint>, Long> times,
            IDebugContext debug) {
        if(!debug.isEnabled(Level.Info)) {
            return;
        }
        final Class<? extends IConstraint> key = c.getClass();
        final long t = times.getOrDefault(key, 0L).longValue() + dt;
        times.put(key, t);
    }

    private void logTimes(String name, Map<Class<? extends IConstraint>, Long> times, IDebugContext debug) {
        debug.info("# ----- {} -----", name);
        for(Map.Entry<Class<? extends IConstraint>, Long> entry : times.entrySet()) {
            debug.info("{} : {}x", entry.getKey().getSimpleName(), entry.getValue());
        }
        debug.info("# ----- {} -----", "-");
    }

    public Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final IDebugContext debug) throws InterruptedException, Delay {
        return entails(state, constraints, completeness, ImmutableSet.of(), debug);
    }

    public Optional<SolverResult> entails(final State state, final Iterable<IConstraint> constraints,
            final Completeness completeness, final Iterable<ITermVar> _localVars, final IDebugContext debug)
            throws InterruptedException, Delay {
        if(debug.isEnabled(Level.Info)) {
            debug.info("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        final Set<ITermVar> localVars = ImmutableSet.copyOf(_localVars);
        final Set<ITermVar> rigidVars = Sets.difference(state.vars(), localVars);
        final SolverResult result = solve(state, constraints, completeness, rigidVars::contains,
                state.scopes()::contains, debug.subContext());
        if(result.hasErrors()) {
            debug.info("Constraints not entailed");
            return Optional.empty();
        } else if(result.delays().isEmpty()) {
            debug.info("Constraints entailed");
            return Optional.of(result);
        } else {
            debug.info("Cannot decide constraint entailment (unsolved constraints)");
            throw result.delay().retainAll(state.vars(), state.scopes());
        }

    }

    private static void printTrace(IConstraint failed, IUnifier.Immutable unifier, IDebugContext debug) {
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(ModuleSolver.shallowTermFormatter(unifier)));
            constraint = constraint.cause().orElse(null);
        }
    }

    private static String toString(Iterable<IConstraint> constraints, IUnifier.Immutable unifier) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(constraint.toString(ModuleSolver.shallowTermFormatter(unifier)));
        }
        return sb.toString();
    }

    @Value.Immutable
    public static abstract class ASolverResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract Completeness completeness();

        @Value.Parameter public abstract Set<IConstraint> errors();

        public boolean hasErrors() {
            return !errors().isEmpty();
        }

        @Value.Parameter public abstract Map<IConstraint, Delay> delays();

        public Delay delay() {
            ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
            ImmutableSet.Builder<CriticalEdge> scopes = ImmutableSet.builder();
            delays().values().stream().forEach(d -> {
                vars.addAll(d.vars());
                scopes.addAll(d.criticalEdges());
            });
            return new Delay(vars.build(), scopes.build());
        }

    }

    public static TermFormatter shallowTermFormatter(final IUnifier.Immutable unifier) {
        return new UnifierFormatter(unifier, 3);
    }

}