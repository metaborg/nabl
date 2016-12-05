package org.metaborg.meta.nabl2.solver;

public class TypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TypeException() {
        super();
    }

    public TypeException(String message) {
        super(message);
    }

    public TypeException(Throwable cause) {
        super(cause);
    }

    public TypeException(String message, Throwable cause) {
        super(message, cause);
    }
    
}