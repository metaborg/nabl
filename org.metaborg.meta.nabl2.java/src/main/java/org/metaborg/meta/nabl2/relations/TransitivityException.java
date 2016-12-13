package org.metaborg.meta.nabl2.relations;

public class TransitivityException extends RelationException {

    private static final long serialVersionUID = 1L;

    public TransitivityException() {
    }

    public TransitivityException(String message) {
        super(message);
    }

    public TransitivityException(Throwable cause) {
        super(cause);
    }

    public TransitivityException(String message, Throwable cause) {
        super(message, cause);
    }

}
