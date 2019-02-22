package mb.statix.concurrent.solver;

import org.metaborg.util.functions.Predicate1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.Completeness;
import mb.statix.solver.ConstraintContext;
import mb.statix.solver.log.IDebugContext;

public class ConcurrentConstraintContext extends ConstraintContext {

    public ConcurrentConstraintContext(ConcurrentCompleteness completeness, IDebugContext debug) {
        super(completeness, v -> false, s -> false, debug);
    }

    public ConcurrentConstraintContext(ConcurrentCompleteness completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed, IDebugContext debug) {
        super(completeness, isRigid, isClosed, debug);
    }

    @Override
    public Completeness completeness() {
        return ((ConcurrentCompleteness) super.completeness()).freeze();
    }
}
