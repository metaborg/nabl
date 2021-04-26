package mb.statix.solver;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;

public class ConstraintContext {

    private final IsComplete isComplete;
    private final IDebugContext debug;

    public ConstraintContext(IsComplete isComplete, IDebugContext debug) {
        this.isComplete = isComplete;
        this.debug = debug;
    }

    public IDebugContext debug() {
        return debug;
    }

    public boolean isComplete(Scope scope, EdgeOrData<ITerm> label, IState state) {
        return isComplete.test(scope, label, state);
    }

    public boolean isClosed(Scope scope, IState state) {
        return !state.scopes().contains(scope);
    }

    public boolean isRigid(ITermVar var, IState state) {
        return !state.vars().contains(var);
    }

}