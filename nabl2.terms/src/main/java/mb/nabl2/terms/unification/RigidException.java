package mb.nabl2.terms.unification;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;

public class RigidException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;

    public RigidException(ITermVar var) {
        super("rigid", null, false, false);
        this.vars = CapsuleUtil.immutableSet(var);
    }

    public RigidException(ITermVar var1, ITermVar var2) {
        super("rigid", null, false, false);
        this.vars = CapsuleUtil.immutableSet(var1, var2);
    }

    public RigidException(Set.Immutable<ITermVar> vars) {
        super("rigid", null, false, false);
        this.vars = vars;
    }

    public Set<ITermVar> vars() {
        return vars;
    }

    @Override public String getMessage() {
        return "Rigid " + vars;
    }

}