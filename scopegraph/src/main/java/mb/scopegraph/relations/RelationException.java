package mb.scopegraph.relations;

public class RelationException extends Exception {

    private static final long serialVersionUID = 1L;

    public RelationException(String message) {
        this(message, null);
    }

    public RelationException(String message, Throwable cause) {
        super(message, cause);
    }

}