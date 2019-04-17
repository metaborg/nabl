package mb.statix.solver;

import org.metaborg.util.functions.Predicate3;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.log.IDebugContext;

public class ConstraintContext {

    private final Predicate3<ITerm, ITerm, State> isComplete;
    private final IDebugContext debug;

    public ConstraintContext(Predicate3<ITerm, ITerm, State> isComplete, IDebugContext debug) {
        this.isComplete = isComplete;
        this.debug = debug;
    }

    public IDebugContext debug() {
        return debug;
    }

    public boolean isComplete(ITerm scope, ITerm label, State state) {
        return isComplete.test(scope, label, state);
    }

    public boolean isRigid(ITermVar var, State state) {
        return !state.vars().contains(var);
    }

    public boolean isClosed(ITerm scope, State state) {
        return !state.scopes().contains(scope);
    }

}