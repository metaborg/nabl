package mb.statix.solver;

import org.metaborg.util.functions.Predicate1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.log.IDebugContext;

public class ConstraintContext {

    private final Completeness completeness;
    private final Predicate1<ITermVar> isRigid;
    private final Predicate1<ITerm> isClosed;
    private final IDebugContext debug;

    public ConstraintContext(Completeness completeness, IDebugContext debug) {
        this(completeness, v -> false, s -> false, debug);
    }

    public ConstraintContext(Completeness completeness, Predicate1<ITermVar> isRigid, Predicate1<ITerm> isClosed,
            IDebugContext debug) {
        this.completeness = completeness;
        this.isRigid = isRigid;
        this.isClosed = isClosed;
        this.debug = debug;
    }

    public IDebugContext debug() {
        return debug;
    }

    public Completeness completeness() {
        return completeness;
    }

    public boolean isRigid(ITermVar var) {
        return isRigid.test(var);
    }

    public boolean isClosed(ITerm scope) {
        //TODO IMPORTANT isClosed can be "not scopes of other owner and not my own scopes"
        return isClosed.test(scope);
    }

}