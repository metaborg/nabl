package mb.statix.concurrent._attic;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.IState;
import mb.statix.solver.log.IDebugContext;

public class ConcurrentConstraintContext extends ConstraintContext {

    public ConcurrentConstraintContext(IDebugContext debug) {
        super((s, l, st) -> {
            throw new UnsupportedOperationException("ConcurrentConstraintContext::isComplete not supported.");
        }, debug);
    }

    @Override public boolean isComplete(Scope scope, EdgeOrData<ITerm> label, IState state) {
        throw new UnsupportedOperationException("ConcurrentConstraintContext::isComplete not supported.");
    }

    @Override public boolean isClosed(Scope scope, IState state) {
        throw new UnsupportedOperationException("Method ConcurrentConstraintContext::isClosed not implemented.");
    }

}