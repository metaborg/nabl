package mb.statix.solver.persistent;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.p_raffrayi.ITypeCheckerContext;
import mb.scopegraph.oopsla20.INameResolution;
import mb.scopegraph.oopsla20.reference.FastNameResolution;
import mb.statix.concurrent.StatixSolver;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.tracer.EmptyTracer;
import mb.statix.solver.tracer.EmptyTracer.Empty;
import mb.statix.solver.tracer.SolverTracer;
import mb.statix.solver.tracer.SolverTracer.IResult;
import mb.statix.spec.PreSolvedConstraint;
import mb.statix.spec.Spec;

public class Solver {

    // flags
    public static final int RETURN_ON_FIRST_ERROR = 1;
    public static final int FORCE_INTERP_QUERIES = 2;

    public static final int TERM_FORMAT_DEPTH = 4;

    public static final boolean INCREMENTAL_CRITICAL_EDGES = true;

    public static final int ERROR_TRACE_TERM_DEPTH = 4;

    public static final io.usethesource.capsule.Set.Immutable<ITermVar> NO_UPDATED_VARS = CapsuleUtil.immutableSet();
    public static final ImList.Immutable<IConstraint> NO_NEW_CONSTRAINTS = ImList.Immutable.of();
    public static final Completeness.Immutable NO_NEW_CRITICAL_EDGES =
            Completeness.Immutable.of();
    public static final io.usethesource.capsule.Map.Immutable<ITermVar, ITermVar> NO_EXISTENTIALS = CapsuleUtil.immutableMap();

    private Solver() {
    }

    public static SolverResult<Empty> solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IDebugContext debug, ICancel cancel, IProgress progress, int flags) throws InterruptedException {
        return solve(spec, state, constraint, (s, l, st) -> true, debug, cancel, progress, flags);
    }

    public static <TR extends IResult<TR>> SolverResult<TR> solve(final Spec spec, final IState.Immutable state,
            final IConstraint constraint, final IsComplete isComplete, final IDebugContext debug, final ICancel cancel,
            IProgress progress, SolverTracer<TR> tracer, int flags)
            throws InterruptedException {
        return new GreedySolver<>(spec, state, constraint, isComplete, debug, progress, cancel, tracer, flags).solve();
    }

    public static SolverResult<Empty> solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, final ICancel cancel, IProgress progress, int flags)
            throws InterruptedException {
        return solve(spec, state, constraint, isComplete, debug, cancel, progress, new EmptyTracer(), flags);
    }

    public static SolverResult<Empty> solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final ICompleteness.Immutable completeness, final IDebugContext debug, ICancel cancel, IProgress progress,
            int flags) throws InterruptedException {
        return solve(spec, state, Collections.singletonList(constraint), Collections.emptyMap(), completeness,
                (s, l, st) -> true, debug, progress, cancel, flags);
    }

    public static <TR extends IResult<TR>> SolverResult<TR> solve(final Spec spec, final IState.Immutable state,
            final Iterable<IConstraint> constraints, final Map<IConstraint, Delay> delays,
            final ICompleteness.Immutable completeness, final IsComplete isComplete, final IDebugContext debug,
            IProgress progress, ICancel cancel, SolverTracer<TR> tracer, int flags) throws InterruptedException {
        return new GreedySolver<>(spec, state, constraints, delays, completeness, isComplete, debug, progress, cancel,
                tracer, flags).solve();
    }

    public static SolverResult<Empty> solve(final Spec spec, final IState.Immutable state,
            final Iterable<IConstraint> constraints, final Map<IConstraint, Delay> delays,
            final ICompleteness.Immutable completeness, final IsComplete isComplete, final IDebugContext debug,
            IProgress progress, ICancel cancel, int flags) throws InterruptedException {
        return solve(spec, state, constraints, delays, completeness, isComplete, debug, progress, cancel,
                new EmptyTracer(), flags);
    }

    public static IFuture<SolverResult<Empty>> solveConcurrent(IConstraint constraint, Spec spec,
            IState.Immutable state, ICompleteness.Immutable completeness, IDebugContext debug, IProgress progress,
            ICancel cancel, ITypeCheckerContext<Scope, ITerm, ITerm> scopeGraph, int flags,
            Iterable<Scope> rootScopes) {
        return new StatixSolver<>(constraint, spec, state, completeness, debug, progress, cancel, scopeGraph,
                new EmptyTracer(), flags).solve(rootScopes);
    }

    public static boolean entails(final Spec spec, IState.Immutable state, final Iterable<IConstraint> constraints,
            final Map<IConstraint, Delay> delays, final ICompleteness.Immutable completeness,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws Delay, InterruptedException {
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Checking entailment of {}", toString(constraints, state.unifier()));
        }
        final IState.Immutable subState = state.subState();

        final PreSolveResult preSolveResult;
        if((preSolveResult =
                Solver.preEntail(subState, completeness, Constraints.conjoin(constraints)).orElse(null)) == null) {
            return false;
        }
        if(preSolveResult.constraints.isEmpty()) {
            return entailed(subState, preSolveResult, debug);
        }

        final SolverResult<Empty> result = Solver.solve(spec, preSolveResult.state, preSolveResult.constraints, delays,
                preSolveResult.criticalEdges, isComplete, debug.subContext(), progress, cancel, RETURN_ON_FIRST_ERROR);

        return Solver.entailed(subState, result, debug);
    }

    public static boolean entails(final Spec spec, IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws Delay, InterruptedException {
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Checking entailment of {}", toString(constraint, state.unifier()));
        }
        final IState.Immutable subState = state.subState();

        final PreSolveResult preSolveResult;
        if((preSolveResult =
                Solver.preEntail(subState, Completeness.Immutable.of(), constraint).orElse(null)) == null) {
            return false;
        }
        if(preSolveResult.constraints.isEmpty()) {
            return entailed(subState, preSolveResult, debug);
        }

        final SolverResult<Empty> result = Solver.solve(spec, preSolveResult.state, preSolveResult.constraints,
                Collections.emptyMap(), preSolveResult.criticalEdges, isComplete, debug.subContext(), progress, cancel,
                RETURN_ON_FIRST_ERROR);

        return entailed(subState, result, debug);
    }

    public static <R extends SolverTracer.IResult<R>> boolean entailed(IState.Immutable initialState,
            SolverResult<R> result, final IDebugContext debug) throws Delay {
        if(!initialState.vars().isEmpty() || !initialState.scopes().isEmpty()) {
            throw new IllegalArgumentException("Incurrent initial state: create with IState::subState.");
        }

        if(result.hasErrors()) {
            // no entailment if errors
            debug.debug("Constraints not entailed: errors");
            return false;
        }

        final IState.Immutable newState = result.state();
        final Delay delay = result.delays().isEmpty() ? null : result.delay();
        return entailed(initialState, newState, delay, debug);
    }

    public static boolean entailed(IState.Immutable initialState, PreSolveResult preSolveResult,
            final IDebugContext debug) {
        try {
            return entailed(initialState, preSolveResult.state, null, debug);
        } catch(Delay d) {
            throw new IllegalStateException(d);
        }
    }

    private static boolean entailed(IState.Immutable initialState, IState.Immutable newState,
            final @Nullable Delay delay, final IDebugContext debug) throws Delay {
        if(!initialState.vars().isEmpty() || !initialState.scopes().isEmpty()) {
            throw new IllegalArgumentException("Incurrent initial state: create with IState::subState.");
        }
        debug.debug("Checking entailment");

        final Set<ITermVar> newVars = newState.vars();
        final Set<Scope> newScopes = newState.scopes();
        final IUniDisunifier.Immutable newUnifier = newState.unifier();

        if(delay != null) {
            final Delay _delay = delay.removeAll(newVars, newScopes);
            if(_delay.criticalEdges().isEmpty() && _delay.vars().isEmpty()) {
                debug.debug("Constraints not entailed: internal stuckness"); // of the point-free mind
                return false;
            } else {
                debug.debug("Cannot decide constraint entailment: unsolved constraints");
                throw _delay;
            }
        }

        if(newUnifier.disequalities().stream().flatMap(diseq -> diseq.domainSet().stream()).filter(newVars::contains)
                .count() > 0) {
            // if any local variables are still constrained, the entailment does not hold for all possible assignments for the free local variables
            debug.debug("Constraints not entailed: internal stuckness"); // of the point-free mind
            return false;
        }

        debug.debug("Constraints entailed");
        return true;
    }

    static void printTrace(IConstraint failed, IUniDisunifier.Immutable unifier, IDebugContext debug) {
        @Nullable IConstraint constraint = failed;
        while(constraint != null) {
            debug.error(" * {}", constraint.toString(Solver.shallowTermFormatter(unifier, TERM_FORMAT_DEPTH)));
            constraint = constraint.cause().orElse(null);
        }
    }

    public static String toString(IConstraint constraint, IUniDisunifier.Immutable unifier) {
        return constraint.toString(Solver.shallowTermFormatter(unifier, TERM_FORMAT_DEPTH));
    }

    public static String toString(Iterable<IConstraint> constraints, IUniDisunifier.Immutable unifier) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(constraint.toString(Solver.shallowTermFormatter(unifier, TERM_FORMAT_DEPTH)));
        }
        return sb.toString();
    }

    public static INameResolution.Builder<Scope, ITerm, ITerm> nameResolutionBuilder() {
        return FastNameResolution.builder();

    }

    public static TermFormatter shallowTermFormatter(final IUniDisunifier unifier, int depth) {
        // @formatter:off
        return new UnifierFormatter(unifier, depth, (t, u, f) -> M.cases(
            Scope.matcher(),
            TermIndex.matcher()
        ).map(ITerm::toString).match(t, unifier));
        // @formatter:on
    }

    ///////////////////////////////////////////////////////////////////////////

    public static Optional<PreSolveResult> preEntail(final IState.Immutable state,
            final @Nullable ICompleteness.Immutable criticalEdges, final IConstraint constraint) throws Delay {
        IState.Transient _state = state.melt();
        IUniDisunifier.Transient _unifier = state.unifier().melt();
        java.util.Set<ITermVar> _updatedVars = new HashSet<>();
        List<IConstraint> constraints = new ArrayList<>();
        ICompleteness.Transient _completeness =
                criticalEdges != null ? criticalEdges.melt() : Completeness.Transient.of();
        java.util.Map<ITermVar, ITermVar> _existentials = new HashMap<>();
        List<IConstraint> failures = new ArrayList<>();
        Map<IConstraint, Delay> delays = new HashMap<>();
        PreSolvedConstraint.preSolve(constraint, _state::freshVars, _unifier, v -> !_state.vars().contains(v),
                _updatedVars, constraints, _completeness, _existentials, failures, delays,
                constraint.cause().orElse(null), true);
        if(!failures.isEmpty()) {
            return Optional.empty();
        }
        if(!delays.isEmpty()) {
            throw Delay.of(delays.values());
        }
        final IState.Immutable newState = _state.freeze().withUnifier(_unifier.freeze());
        final PreSolveResult preSolveResult = new PreSolveResult(newState, _updatedVars, constraints,
                _completeness.freeze(), _existentials, Collections.emptyMap());
        return Optional.of(preSolveResult);
    }

    public static class PreSolveResult {
        public final IState.Immutable state;
        public final Set<ITermVar> updatedVars;
        public final List<IConstraint> constraints;
        public final ICompleteness.Immutable criticalEdges;
        public final Map<ITermVar, ITermVar> existentials;
        public final Map<IConstraint, IMessage> errors;

        public PreSolveResult(IState.Immutable state, Set<ITermVar> updatedVars, List<IConstraint> constraints,
                ICompleteness.Immutable criticalEdges, Map<ITermVar, ITermVar> existentials,
                Map<IConstraint, IMessage> errors) {
            this.state = state;
            this.updatedVars = updatedVars;
            this.constraints = constraints;
            this.criticalEdges = criticalEdges;
            this.existentials = existentials;
            this.errors = errors;
        }

    }

}
