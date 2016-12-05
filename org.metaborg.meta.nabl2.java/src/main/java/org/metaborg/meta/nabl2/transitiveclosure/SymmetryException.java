package org.metaborg.meta.nabl2.transitiveclosure;

public class SymmetryException extends Exception {

    private static final long serialVersionUID = 1L;

    public SymmetryException() {
    }

    public SymmetryException(String message) {
        super(message);
    }

    public SymmetryException(Throwable cause) {
        super(cause);
    }

    public SymmetryException(String message, Throwable cause) {
        super(message, cause);
    }

}
