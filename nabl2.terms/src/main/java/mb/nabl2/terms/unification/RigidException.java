package mb.nabl2.terms.unification;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;

public class RigidException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> vars;

    public RigidException(ITermVar var) {
        super("rigid", null, false, false);
        this.vars = ImmutableSet.of(var);
    }

    public RigidException(ITermVar var1, ITermVar var2) {
        super("rigid", null, false, false);
        this.vars = ImmutableSet.of(var1, var2);
    }

    public RigidException(Iterable<ITermVar> vars) {
        super("rigid", null, false, false);
        this.vars = ImmutableSet.copyOf(vars);
    }

    public Set<ITermVar> vars() {
        return vars;
    }

    @Override public String getMessage() {
        return "Rigid " + vars;
    }

}