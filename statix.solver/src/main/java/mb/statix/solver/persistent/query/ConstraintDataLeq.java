package mb.statix.solver.persistent.query;

import java.util.Collections;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.reference.DataLeq;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.statix.constraints.Constraints;
import mb.statix.solver.Delay;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
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

    public ConstraintDataLeq(Spec spec, IState.Immutable state, IsComplete isComplete, Rule constraint) {
        this.spec = spec;
        this.state = state;
        this.isComplete = isComplete;
        this.constraint = constraint;
    }

    @Override public boolean leq(ITerm datum1, ITerm datum2) throws ResolutionException, InterruptedException {
        try {
            // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
            final ApplyResult applyResult = RuleUtil.apply(
                    state.unifier(),
                    constraint,
                    ImList.Immutable.of(datum1, datum2),
                    null,
                    ApplyMode.STRICT,
                    Safety.UNSAFE,
                    true
            ).orElse(null);
            if(applyResult == null) {
                return false;
            }

            return Solver.entails(spec, state, Constraints.disjoin(applyResult.body()), Collections.emptyMap(),
                    applyResult.criticalEdges(), isComplete, new NullDebugContext(),
                    new NullProgress().subProgress(1), new NullCancel());
        } catch(Delay d) {
            throw new ResolutionDelayException("Data order delayed.", d);
        }
    }

    @Override public boolean alwaysTrue() throws InterruptedException {
        return constraint.isAlways().orElse(false);
    }

    @Override public String toString() {
        return constraint.toString(state.unifier()::toString);
    }

}