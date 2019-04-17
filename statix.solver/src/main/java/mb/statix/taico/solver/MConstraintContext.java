package mb.statix.taico.solver;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.log.IDebugContext;

public class MConstraintContext {

    private final ICompleteness isComplete;
    private final IDebugContext debug;

    public MConstraintContext(ICompleteness isComplete, IDebugContext debug) {
        this.isComplete = isComplete;
        this.debug = debug;
    }

    public IDebugContext debug() {
        return debug;
    }
    
    public CompletenessResult isComplete(ITerm scope, ITerm label, IMState state) {
        return isComplete.apply(scope, label, state);
    }

    public boolean isRigid(ITermVar var, IMState state) {
        return !state.vars().contains(var);
    }

    public boolean isClosed(ITerm scope, IMState state) {
        return !state.scopes().contains(scope) && !state.scopeGraph().getExtensibleScopes().contains(scope);
    }

}
