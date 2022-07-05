package mb.statix.solver.tracer;

import java.io.Serializable;

import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.resolution.StateMachine;
import mb.statix.constraints.IResolveQuery;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.IState.Immutable;

public abstract class SolverTracer<R extends SolverTracer.IResult<R>> {

    /**
     * Attention: this method can be called by multiple threads at the same time. In addition, the returned instance can
     * be shared with a different thread, implying that all other methods can be called concurrently as well. Statefull
     * tracers should therefore do proper synchronization.
     */
    public abstract SolverTracer<R> subTracer();

    public abstract R result();

    // Constraint solving iteration

    public void onTrySolveConstraint(IConstraint constraint, IState.Immutable state) {

    }

    public void onConstraintSolved(IConstraint constraint, IState.Immutable state) {

    }

    public void onConstraintDelayed(IConstraint constraint, IState.Immutable state) {

    }

    public void onConstraintFailed(IConstraint constraint, IState.Immutable state) {

    }

    // Query Resolution

    public void startQuery(IResolveQuery c, Scope scope, LabelWf<ITerm> labelWF, LabelOrder<ITerm> labelOrder,
            DataWf<Scope, ITerm, ITerm> dataWF, DataLeq<Scope, ITerm, ITerm> dataEquiv,
            DataWf<Scope, ITerm, ITerm> dataWFInternal, DataLeq<Scope, ITerm, ITerm> dataEquivInternal) {

    }

    public void startQuery(IResolveQuery c, Scope scope, StateMachine<ITerm> stateMachine,
            DataWf<Scope, ITerm, ITerm> dataWF, DataLeq<Scope, ITerm, ITerm> dataEquiv,
            DataWf<Scope, ITerm, ITerm> dataWFInternal, DataLeq<Scope, ITerm, ITerm> dataEquivInternal) {
        // TODO Auto-generated method stub

    }

    public interface IResult<SELF> extends Serializable {
        SELF combine(SELF other);
    }

    protected abstract class SubTracer extends SolverTracer<R> {

        @Override public SolverTracer<R> subTracer() {
            return this;
        }

        @Override public void onTrySolveConstraint(IConstraint constraint, Immutable state) {
            SolverTracer.this.onTrySolveConstraint(constraint, state);
        }

        @Override public void onConstraintSolved(IConstraint constraint, Immutable state) {
            SolverTracer.this.onConstraintSolved(constraint, state);
        }

        @Override public void onConstraintDelayed(IConstraint constraint, Immutable state) {
            SolverTracer.this.onTrySolveConstraint(constraint, state);
        }

        @Override public void onConstraintFailed(IConstraint constraint, Immutable state) {
            SolverTracer.this.onConstraintFailed(constraint, state);
        }

    }

}
