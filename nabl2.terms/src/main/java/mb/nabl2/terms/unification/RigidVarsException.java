package mb.nabl2.terms.unification;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;

public class RigidVarsException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> vars;

    public RigidVarsException(ITermVar... vars) {
        this(ImmutableSet.copyOf(vars));
    }

    public RigidVarsException(Iterable<ITermVar> vars) {
        super("Rigid " + vars);
        this.vars = ImmutableSet.copyOf(vars);
    }

    public Set<ITermVar> vars() {
        return vars;
    }

}