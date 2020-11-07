package mb.statix.solver.persistent.query;

import static mb.nabl2.terms.build.TermBuild.B;

import org.metaborg.util.log.Level;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

class ConstraintDataWF implements DataWF<ITerm> {

    private final Spec spec;
    private final Rule constraint;
    private final IState.Immutable state;
    private final IsComplete isComplete;
    private final IDebugContext debug;
    private final IProgress progress;
    private final ICancel cancel;

    public ConstraintDataWF(Spec spec, Rule constraint, IState.Immutable state, IsComplete isComplete,
            IDebugContext debug, IProgress progress, ICancel cancel) {
        this.spec = spec;
        this.constraint = constraint;
        this.state = state;
        this.isComplete = isComplete;
        this.debug = debug;
        this.progress = progress;
        this.cancel = cancel;
    }

    @Override public boolean wf(ITerm datum) throws ResolutionException, InterruptedException {
        final IUniDisunifier.Immutable unifier = state.unifier();
        try {
            final ApplyResult result;
            if((result = RuleUtil.apply(state.unifier(), constraint, ImmutableList.of(datum), null, ApplyMode.STRICT)
                    .orElse(null)) == null) {
                return false;
            }
            if(Solver.entails(spec, state, result.body(), isComplete, debug, progress.subProgress(1), cancel)) {
                if(debug.isEnabled(Level.Debug)) {
                    debug.debug("Well-formed {}", unifier.toString(B.newTuple(datum)));
                }
                return true;
            } else {
                if(debug.isEnabled(Level.Debug)) {
                    debug.debug("Not well-formed {}", unifier.toString(B.newTuple(datum)));
                }
                return false;
            }
        } catch(Delay d) {
            throw new ResolutionDelayException("Data well-formedness delayed.", d);
        }
    }

    @Override public String toString() {
        return constraint.toString(state.unifier()::toString);
    }

}