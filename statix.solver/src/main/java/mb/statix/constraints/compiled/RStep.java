package mb.statix.constraints.compiled;

import java.io.Serializable;
import java.util.Objects;

public final class RStep implements Serializable {

    private static final long serialVersionUID = 1L;

    private final RVar var;
    private final RExp exp;

    public RStep(RVar var, RExp exp) {
        this.var = var;
        this.exp = exp;
    }

    public RVar getVar() {
        return var;
    }

    public RExp getExp() {
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
        final RStep other = (RStep) obj;
        return Objects.equals(var, other.var) && Objects.equals(exp, other.exp);
    }

    @Override public int hashCode() {
        return Objects.hash(var, exp);
    }

}
