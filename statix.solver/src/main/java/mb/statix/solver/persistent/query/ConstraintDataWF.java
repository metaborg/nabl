package mb.statix.solver.persistent.query;

import java.util.Collections;

import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.reference.DataWF;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.statix.constraints.Constraints;
import mb.statix.solver.Delay;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.State;
import mb.statix.solver.query.ResolutionDelayException;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyMode.Safety;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

class ConstraintDataWF implements DataWF<ITerm> {

    private final Spec spec;
    private final Rule constraint;

    private final IState.Immutable state;

    public ConstraintDataWF(Spec spec, Rule constraint) {
        this.spec = spec;
        this.constraint = constraint;
        this.state = State.of();
    }

    @Override public boolean wf(ITerm datum) throws ResolutionException, InterruptedException {
        try {
            final ApplyResult applyResult;
            // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
            if((applyResult = RuleUtil
                    .apply(state.unifier(), constraint, ImmutableList.of(datum), null, ApplyMode.STRICT, Safety.UNSAFE)
                    .orElse(null)) == null) {
                return false;
            }

            return Solver.entails(spec, state, Constraints.disjoin(applyResult.body()), Collections.emptyMap(),
                    applyResult.criticalEdges(), IsComplete.ALWAYS, new NullDebugContext(),
                    new NullProgress().subProgress(1), new NullCancel());
        } catch(Delay d) {
            throw new ResolutionDelayException("Data well-formedness delayed.", d);
        }
    }

    @Override public String toString() {
        return constraint.toString(state.unifier()::toString);
    }

}