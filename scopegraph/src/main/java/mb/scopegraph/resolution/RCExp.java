package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Objects;

import mb.scopegraph.oopsla20.reference.ResolutionException;

public class RCExp<L> implements RExp<L>, Serializable {

    private static final long serialVersionUID = 1L;

    private final RVar env;

    private final RExp<L> exp;

    public RCExp(RVar env, RExp<L> exp) {
        this.env = env;
        this.exp = exp;
    }

    public RVar env() {
        return env;
    }

    public RExp<L> exp() {
        return exp;
    }

    @Override public <R> R match(Cases<L, R> cases) {
        return cases.caseCExp(env, exp);
    }

    @Override public <R, E extends Throwable> R matchInResolution(ResolutionCases<L, R> cases)
            throws ResolutionException, InterruptedException {
        return cases.caseCExp(env, exp);
    }

    @Override public String toString() {
        return "if not empty " + env + " else " + exp;
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked") final RCExp<L> other = (RCExp<L>) obj;
        return Objects.equals(env, other.env) && Objects.equals(exp, other.exp);
    }

    @Override public int hashCode() {
        return Objects.hash(env, exp);
    }

}
