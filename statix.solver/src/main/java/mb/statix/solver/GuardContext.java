package mb.statix.solver;

import org.metaborg.util.functions.Predicate1;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.log.IDebugContext;

public class GuardContext {

    private final Predicate1<ITermVar> isRigid;
    private final IDebugContext debug;

    public GuardContext(IDebugContext debug) {
        this(v -> false, debug);
    }

    public GuardContext(Predicate1<ITermVar> isRigid, IDebugContext debug) {
        this.isRigid = isRigid;
        this.debug = debug;
    }

    public IDebugContext debug() {
        return debug;
    }

    public boolean isRigid(ITermVar var) {
        return isRigid.test(var);
    }

}