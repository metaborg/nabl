package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Objects;

import mb.scopegraph.oopsla20.reference.ResolutionException;

public final class RSubEnv<L> implements RExp<L>, Serializable {

    private static final long serialVersionUID = 1L;

    private final L label;

    private final String state; // TODO: can we 'inline' the proper state here?

    public RSubEnv(L label, String state) {
        this.label = label;
        this.state = state;
    }

    public L label() {
        return label;
    }

    public String state() {
        return state;
    }

    @Override public <R> R match(Cases<L, R> cases) {
        return cases.caseSubEnv(label, state);
    }

    @Override public <R, E extends Throwable> R matchInResolution(ResolutionCases<L, R> cases)
            throws ResolutionException, InterruptedException {
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

        @SuppressWarnings("unchecked") final RSubEnv<L> other = (RSubEnv<L>) obj;
        return Objects.equals(label, other.label) && Objects.equals(state, other.state);
    }

    @Override public int hashCode() {
        return Objects.hash(label, state);
    }

}
