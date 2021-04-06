package mb.statix.solver.persistent.query;

import java.util.Collections;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.Solver.ApplyInStateResult;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyMode.Safety;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

class ConstraintDataLeq implements DataLeq<ITerm> {

    private final Spec spec;
    private final Rule constraint;
    private final IState.Immutable state;
    private final IsComplete isComplete;
    private final IDebugContext debug;
    private final IProgress progress;
    private final ICancel cancel;
    private volatile Boolean alwaysTrue;

    public ConstraintDataLeq(Spec spec, Rule constraint, IState.Immutable state, IsComplete isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel) {
        this.spec = spec;
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
        this.progress = progress;
        this.cancel = cancel;
    }

    @Override public boolean leq(ITerm datum1, ITerm datum2) throws ResolutionException, InterruptedException {
        final IUniDisunifier.Immutable unifier = state.unifier();
        try {
            final ApplyResult applyResult;
            // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
            if((applyResult = RuleUtil.apply(state.unifier(), constraint, ImmutableList.of(datum1, datum2), null,
                    ApplyMode.STRICT, Safety.UNSAFE).orElse(null)) == null) {
                return false;
            }

            final ApplyInStateResult bodyResult;
            if((bodyResult = Solver.applyInState(state, applyResult.criticalEdges(), applyResult.body(), Safety.UNSAFE)
                    .orElse(null)) == null) {
                return false;
            }

            if(Solver.entails(spec, bodyResult.state, Constraints.disjoin(bodyResult.constraint),
                    Collections.emptyMap(), bodyResult.criticalEdges, isComplete, debug, progress.subProgress(1),
                    cancel)) {
                if(debug.isEnabled(Level.Debug)) {
                    debug.debug("{} shadows {}", unifier.toString(datum1), unifier.toString(datum2));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Debug)) {
                    debug.debug("{} does not shadow {}", unifier.toString(datum1), unifier.toString(datum2));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data order delayed.", d);
        }
    }

    @Override public boolean alwaysTrue() throws InterruptedException {
        if(alwaysTrue != null)
            return alwaysTrue.booleanValue();

        return alwaysTrue = constraint.isAlways().orElse(false);
    }

    @Override public String toString() {
        return constraint.toString(state.unifier()::toString);
    }

}