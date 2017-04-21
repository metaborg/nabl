package org.metaborg.meta.nabl2.solver;

public class FunctionUndefinedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FunctionUndefinedException(String message) {
        super(message);
    }

    public FunctionUndefinedException(String message, Throwable cause) {
        super(message, cause);
    }

}