package mb.statix.constraints.compiled;

import java.io.Serializable;
import java.util.Objects;

import mb.nabl2.terms.ITerm;

public final class RSubEnv implements RExp, Serializable {

    private static final long serialVersionUID = 1L;

    private final ITerm label;

    private final String state; // TODO: can we 'inline' the proper state here?

    public RSubEnv(ITerm label, String state) {
        this.label = label;
        this.state = state;
    }

    public ITerm label() {
        return label;
    }

    public String state() {
        return state;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseSubEnv(label, state);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseSubEnv(label, state);
    }

    @Override public String toString() {
        return "SubEnv " + label + " " + state;
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        final RSubEnv other = (RSubEnv) obj;
        return Objects.equals(label, other.label) && Objects.equals(state, other.state);
    }

    @Override public int hashCode() {
        return Objects.hash(label, state);
    }

}
