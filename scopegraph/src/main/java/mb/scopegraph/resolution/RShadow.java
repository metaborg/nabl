package mb.scopegraph.resolution;

import java.io.Serializable;
import java.util.Objects;

public class RShadow<L> implements RExp<L>, Serializable {

    private static final long serialVersionUID = 1L;

    private final RVar left;

    private final RVar right;

    public RShadow(RVar left, RVar right) {
        this.left = left;
        this.right = right;
    }

    public RVar left() {
        return left;
    }

    public RVar right() {
        return right;
    }

    @Override public <R> R match(Cases<L, R> cases) {
        return cases.caseShadow(left, right);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<L, R, E> cases) throws E {
        return cases.caseShadow(left, right);
    }

    @Override public String toString() {
        return "shadow(" + left + ", " + right + ")";
    }

    @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked") final RShadow<L> other = (RShadow<L>) obj;
        return Objects.equals(left, other.left) && Objects.equals(right, other.right);
    }

    @Override public int hashCode() {
        return Objects.hash(left, right);
    }

}
