package mb.nabl2.terms.unification;

import mb.nabl2.terms.ITerm;

public class UnificationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ITerm left;
    private final ITerm right;

    public UnificationException(ITerm left, ITerm right) {
        super("Cannot unify " + left + " with " + right);
        this.left = left;
        this.right = right;
    }

    public ITerm getLeft() {
        return left;
    }

    public ITerm getRight() {
        return right;
    }

}