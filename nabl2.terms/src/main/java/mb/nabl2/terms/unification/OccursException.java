package mb.nabl2.terms.unification;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;

public class OccursException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Set.Immutable<ITermVar> vars;

    public OccursException(Set.Immutable<ITermVar> vars) {
        super("occurs", null, false, false);
        this.vars = vars;
    }

    public Set.Immutable<ITermVar> vars() {
        return vars;
    }

    @Override public String getMessage() {
        return "Recursive " + vars;
    }

}