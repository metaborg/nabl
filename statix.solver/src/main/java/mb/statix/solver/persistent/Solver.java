package mb.statix.solver.persistent;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.Renaming;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.concurrent.solver.StatixSolver;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.scopegraph.INameResolution;
import mb.statix.scopegraph.reference.FastNameResolution;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.ApplyMode.Safety;
import mb.statix.spec.Spec;

public class Solver {

    public static final int RETURN_ON_FIRST_ERROR = 1;

    public static final int TERM_FORMAT_DEPTH = 4;

    public static final boolean INCREMENTAL_CRITICAL_EDGES = true;

    private Solver() {
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IDebugContext debug, ICancel cancel, IProgress progress, int flags) throws InterruptedException {
        return solve(spec, state, constraint, (s, l, st) -> true, debug, cancel, progress, flags);
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, final ICancel cancel, IProgress progress, int flags)
            throws InterruptedException {
        final Optional<SolverResult> trivial = solveTrivial(constraint, spec, state, Completeness.Immutable.of());
        if(trivial.isPresent()) {
            return trivial.get();
        }
        return new GreedySolver(spec, state, constraint, isComplete, debug, progress, cancel, flags).solve();
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state, final IConstraint constraint,
            final ICompleteness.Immutable completeness, final IDebugContext debug, ICancel cancel, IProgress progress,
            int flags) throws InterruptedException {
        return solve(spec, state, Collections.singletonList(constraint), Collections.emptyMap(), completeness,
                (s, l, st) -> true, debug, progress, cancel, flags);
    }

    public static SolverResult solve(final Spec spec, final IState.Immutable state,
            final Iterable<IConstraint> constraints, final Map<IConstraint, Delay> delays,
            final ICompleteness.Immutable completeness, final IsComplete isComplete, final IDebugContext debug,
            IProgress progress, ICancel cancel, int flags) throws InterruptedException {
        final Optional<SolverResult> trivial = solveTrivial(constraints, spec, state, completeness);
        if(trivial.isPresent()) {
            return trivial.get();
        }
        return new GreedySolver(spec, state, constraints, delays, completeness, isComplete, debug, progress, cancel,
                flags).solve();
    }

    public static IFuture<SolverResult> solveConcurrent(IConstraint constraint, Spec spec, IState.Immutable state,
            ICompleteness.Immutable completeness, IDebugContext debug, IProgress progress, ICancel cancel,
            ITypeCheckerContext<Scope, ITerm, ITerm> scopeGraph, int flags, Iterable<Scope> rootScopes) {
        Optional<SolverResult> trivial = solveTrivial(constraint, spec, state, completeness);
        if(trivial.isPresent()) {
            return CompletableFuture.completedFuture(trivial.get());
        }
        return new StatixSolver(constraint, spec, state, completeness, debug, progress, cancel, scopeGraph, flags)
                .solve(rootScopes);
    }

    public static Optional<SolverResult> solveTrivial(Iterable<IConstraint> constraints, Spec spec,
            IState.Immutable state, ICompleteness.Immutable completeness) {
        final Iterator<IConstraint> it = constraints.iterator();
        if(!it.hasNext()) {
            return Optional.of(SolverResult.of(spec, state, Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), completeness));
        }
        final IConstraint c = it.next();
        if(it.hasNext()) {
            return Optional.empty();
        }
        return solveTrivial(c, spec, state, completeness);

    }

    public static Optional<SolverResult> solveTrivial(IConstraint constraint, Spec spec, IState.Immutable state,
            ICompleteness.Immutable completeness) {
        return Constraints.trivial(constraint).map(trivial -> {
            final Map<IConstraint, IMessage> messages;
            if(trivial) {
                messages = Collections.emptyMap();
            } else {
                messages = Collections.singletonMap(constraint, MessageUtil.findClosestMessage(constraint));
            }
            return SolverResult.of(spec, state, messages, Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptySet(), Collections.emptySet(), completeness);
        });
    }

    public static boolean entails(final Spec spec, IState.Immutable state, final Iterable<IConstraint> constraints,
            final Map<IConstraint, Delay> delays, final ICompleteness.Immutable completeness,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws Delay, InterruptedException {
        final Optional<Boolean> trivial = entailsTrivial(constraints);
        if(trivial.isPresent()) {
            return trivial.get();
        }

        final IUniDisunifier.Immutable unifier = state.unifier();
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Checking entailment of {}", toString(constraints, unifier));
        }

        final IState.Immutable subState = state.subState();
        final SolverResult result = Solver.solve(spec, subState, constraints, delays, completeness, isComplete,
                debug.subContext(), progress, cancel, RETURN_ON_FIRST_ERROR);
        return Solver.entailed(subState, result, debug);
    }

    public static boolean entails(final Spec spec, IState.Immutable state, final IConstraint constraint,
            final IsComplete isComplete, final IDebugContext debug, IProgress progress, ICancel cancel)
            throws Delay, InterruptedException {
        final Optional<Boolean> trivial = entailsTrivial(constraint);
        if(trivial.isPresent()) {
            return trivial.get();
        }

        final IUniDisunifier.Immutable unifier = state.unifier();
        if(debug.isEnabled(Level.Debug)) {
            debug.debug("Checking entailment of {}", toString(constraint, unifier));
        }

        final IState.Immutable subState = state.subState();
        final SolverResult result = Solver.solve(spec, subState, constraint, isComplete, debug.subContext(), cancel,
                progress, RETURN_ON_FIRST_ERROR);
        return Solver.entailed(subState, result, debug);
    }

    public static boolean entailed(IState.Immutable initialState, SolverResult result, final IDebugContext debug)
            throws Delay {
        if(!initialState.vars().isEmpty() || !initialState.scopes().isEmpty()) {
            throw new IllegalArgumentException("Incurrent initial state: create with IState::subState.");
        }

        debug.debug("Checking entailment");

        if(result.hasErrors()) {
            // no entailment if errors
            debug.debug("Constraints not entailed: errors");
            return false;
        }

        final IState.Immutable newState = result.state();
        final Set<ITermVar> newVars = newState.vars();
        final Set<Scope> newScopes = newState.scopes();
        final IUniDisunifier.Immutable newUnifier = newState.unifier();

        if(!result.delays().isEmpty()) {
            final Delay delay = result.delay().removeAll(newVars, newScopes);
            if(delay.criticalEdges().isEmpty() && delay.vars().isEmpty()) {
                debug.debug("Constraints not entailed: internal stuckness"); // of the point-free mind
                return false;
            } else {
                debug.debug("Cannot decide constraint entailment: unsolved constraints");
                throw delay;
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

    public static Optional<Boolean> entailsTrivial(Iterable<IConstraint> constraints) {
        final Iterator<IConstraint> it = constraints.iterator();
        if(!it.hasNext()) {
            return Optional.of(true);
        }
        final IConstraint c = it.next();
        if(it.hasNext()) {
            return Optional.empty();
        }
        return entailsTrivial(c);

    }

    public static Optional<Boolean> entailsTrivial(IConstraint constraint) {
        return Constraints.trivial(constraint);
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


    public static Tuple2<IState.Immutable, IRenaming> buildExistentials(IState.Immutable state, Set<ITermVar> vars) {
        final Renaming.Builder _existentials = Renaming.builder();
        IState.Immutable newState = state;
        for(ITermVar var : vars) {
            final Tuple2<ITermVar, IState.Immutable> varAndState = newState.freshVar(var);
            final ITermVar freshVar = varAndState._1();
            newState = varAndState._2();
            _existentials.put(var, freshVar);
        }
        final Renaming existentials = _existentials.build();
        return Tuple2.of(newState, existentials);
    }

    public static Optional<ApplyInStateResult> applyInState(final IState.Immutable state,
            final @Nullable ICompleteness.Immutable criticalEdges, final IConstraint body, final Safety safety) {
        return Optional.of(new ApplyInStateResult(state, Collections.emptySet(), body,
                criticalEdges != null ? criticalEdges : Completeness.Immutable.of(), Collections.emptyMap()));
        /*
        final Optional<Boolean> isAlways = body.isAlways();
        if(isAlways.isPresent() && !isAlways.get()) {
            return Optional.empty();
        }
        
        final Tuple2<IState.Immutable, IRenaming> ext = buildExistentials(state, body.vars());
        IState.Immutable newState = ext._1();
        
        final IUniDisunifier.Immutable newUnifier;
        final Set<ITermVar> updatedVars;
        if(body.unifier().isEmpty()) {
            newUnifier = newState.unifier();
            updatedVars = Collections.emptySet();
        } else {
            final IUniDisunifier.Result<IUnifier.Immutable> unifyResult;
            try {
                if((unifyResult =
                        newState.unifier().uniDisunify(body.unifier().rename(ext._2())).orElse(null)) == null) {
                    return Optional.empty();
                }
            } catch(OccursException ex) {
                return Optional.empty();
            }
            newUnifier = unifyResult.unifier();
            updatedVars = unifyResult.result().domainSet();
        }
        newState = newState.withUnifier(newUnifier);
        
        ICompleteness.Immutable newBodyCriticalEdges =
                criticalEdges != null ? criticalEdges : Completeness.Immutable.of();
        if(body.bodyCriticalEdges().isPresent()) {
            newBodyCriticalEdges = newBodyCriticalEdges.addAll(body.bodyCriticalEdges().get(), newUnifier);
        }
        
        IConstraint newConstraint;
        if(isAlways.isPresent() && isAlways.get()) {
            newConstraint = new CTrue();
        } else {
            // unsafeApply : we assume the resource of spec variables is empty and of state variables non-empty
            if(safety.equals(Safety.UNSAFE)) {
                newConstraint = body.constraint().unsafeApply(ext._2().asSubstitution());
            } else {
                newConstraint = body.constraint().apply(ext._2().asSubstitution());
            }
        }
        newConstraint = newConstraint.withCause(body.cause().orElse(null));
        
        return Optional.of(
                new ApplyInStateResult(newState, updatedVars, newConstraint, newBodyCriticalEdges, ext._2().asMap()));
                */
    }

    public static class ApplyInStateResult {
        public final IState.Immutable state;
        public final Set<ITermVar> updatedVars;
        public final IConstraint constraint;
        public final ICompleteness.Immutable criticalEdges;
        public final Map<ITermVar, ITermVar> existentials;

        public ApplyInStateResult(IState.Immutable state, Set<ITermVar> updatedVars, IConstraint constraints,
                ICompleteness.Immutable criticalEdges, Map<ITermVar, ITermVar> existentials) {
            this.state = state;
            this.updatedVars = updatedVars;
            this.constraint = constraints;
            this.criticalEdges = criticalEdges;
            this.existentials = existentials;
        }

    }

}