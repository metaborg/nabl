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
        CompletenessResult result = isComplete.apply(scope, label, state);
//        System.err.println("MCC: isComplete: scope of " + Scopes.getScope(scope).getResource() + ", state of " + state.owner() + ". IsComplete: " + result);
        return result;
    }

    public boolean isRigid(ITermVar var, IMState state) {
        boolean result = !state.vars().contains(var);
//        System.err.println("MCC: isRigid: variable of " + var.getResource() + ", state of " + state.owner() + ". Rigid: " + result);
        return result;
    }

    public boolean isClosed(ITerm scope, IMState state) {
        boolean result = !state.scopes().contains(scope) && !state.scopeGraph().getExtensibleScopes().contains(scope);
//        System.err.println("MCC: isClosed: scope of " + Scopes.getScope(scope).getResource() + ", state of " + state.owner() + ". Closed: " + result);
        return result;
    }

}
