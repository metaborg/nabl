package org.metaborg.meta.nabl2.relations;

public class RelationException extends Exception {

    private static final long serialVersionUID = 1L;

    public RelationException() {
    }

    public RelationException(String message) {
        super(message);
    }

    public RelationException(Throwable cause) {
        super(cause);
    }

    public RelationException(String message, Throwable cause) {
        super(message, cause);
    }

}
