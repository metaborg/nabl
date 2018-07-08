package mb.nabl2.terms.unification;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;

public class OccursException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Set<ITermVar> vars;

    public OccursException(Iterable<ITermVar> vars) {
        super("Recursive " + vars);
        this.vars = ImmutableSet.copyOf(vars);
    }

    public Set<ITermVar> vars() {
        return vars;
    }

}