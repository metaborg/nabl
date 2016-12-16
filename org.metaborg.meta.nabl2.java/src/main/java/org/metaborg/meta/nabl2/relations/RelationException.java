package org.metaborg.meta.nabl2.relations;

public class RelationException extends Exception {

    private static final long serialVersionUID = 1L;

    public RelationException(String message) {
        super(message);
    }

    public RelationException(String message, Throwable cause) {
        super(message, cause);
    }

}