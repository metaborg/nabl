package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.terms.ITerm;

public class UnificationException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnificationException(ITerm left, ITerm right) {
        super("Cannot unify " + left + " with " + right);
    }

    public UnificationException(String message) {
        super(message);
    }

}