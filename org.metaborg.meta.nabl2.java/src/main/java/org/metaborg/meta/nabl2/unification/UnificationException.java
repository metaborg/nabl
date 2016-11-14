package org.metaborg.meta.nabl2.unification;

public abstract class UnificationException extends Exception {

    private static final long serialVersionUID = 1L;


    public UnificationException() {
    }

    public UnificationException(Throwable cause) {
        super(cause);
    }

}