package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Objects;

public final class RStep<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final RVar var;
    private final RExp<L> exp;

    public RStep(RVar var, RExp<L> exp) {
        this.var = var;
        this.exp = exp;
    }

    public RVar getVar() {
        return var;
    }

    public RExp<L> getExp() {
        return exp;
    }

    @Override public String toString() {
        return var + " := " + exp;
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked") final RStep<L> other = (RStep<L>) obj;
        return Objects.equals(var, other.var) && Objects.equals(exp, other.exp);
    }

    @Override public int hashCode() {
        return Objects.hash(var, exp);
    }

}
