package mb.nabl2.terms.unification;

import mb.nabl2.terms.ITerm;

public class CannotUnifyException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ITerm left;
    private final ITerm right;

    public CannotUnifyException(ITerm left, ITerm right) {
        this.left = left;
        this.right = right;
    }

    @Override public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot unify ");
        sb.append(left);
        sb.append(" with ");
        sb.append(right);
        return sb.toString();
    }

    public ITerm getLeft() {
        return left;
    }

    public ITerm getRight() {
        return right;
    }

}