package org.metaborg.meta.nabl2.solver;

public class SolverException extends Exception {

    private static final long serialVersionUID = 42L;

    public SolverException(String message) {
        super(message);
    }

    public SolverException(Throwable cause) {
        super(cause);
    }

    public SolverException(String message, Throwable cause) {
        super(message, cause);
    }

}