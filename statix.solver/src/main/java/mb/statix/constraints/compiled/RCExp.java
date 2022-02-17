package mb.statix.constraints.compiled;

import java.io.Serializable;
import java.util.Objects;

public class RCExp implements RExp, Serializable {

    private static final long serialVersionUID = 1L;

    private final RVar env;

    private final RExp exp;

    public RCExp(RVar env, RExp exp) {
        this.env = env;
        this.exp = exp;
    }

    public RVar env() {
        return env;
    }

    public RExp exp() {
        return exp;
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseCExp(env, exp);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
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

        final RCExp other = (RCExp) obj;
        return Objects.equals(env, other.env) && Objects.equals(exp, other.exp);
    }

    @Override public int hashCode() {
        return Objects.hash(env, exp);
    }

}
