package mb.statix.modular.solver;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.modular.solver.state.IMState;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;

//TODO Extend ConstraintContext instead, only isClosed needs to be changed.
public class MConstraintContext {

    private final IsComplete isComplete;
    private final IDebugContext debug;

    public MConstraintContext(IsComplete isComplete, IDebugContext debug) {
        this.isComplete = isComplete;
        this.debug = debug;
    }

    public IDebugContext debug() {
        return debug;
    }
    
    public boolean isComplete(Scope scope, ITerm label, IState state) {
        return isComplete.test(scope, label, state);
//        TDebug.DEV_OUT.info("MCC: isComplete: scope of " + Scopes.getScope(scope).getResource() + ", state of " + state.owner() + ". IsComplete: " + result);
    }

    public boolean isRigid(ITermVar var, IState state) {
        boolean result = !state.vars().contains(var);
//        TDebug.DEV_OUT.info("MCC: isRigid: variable of " + var.getResource() + ", state of " + state.owner() + ". Rigid: " + result);
        return result;
    }

    public boolean isClosed(Scope scope, IMState state) {
        boolean result = !state.scopes().contains(scope) && !state.scopeGraph().getExtensibleScopes().contains(scope);
//        TDebug.DEV_OUT.info("MCC: isClosed: scope of " + Scopes.getScope(scope).getResource() + ", state of " + state.owner() + ". Closed: " + result);
        return result;
    }

}
