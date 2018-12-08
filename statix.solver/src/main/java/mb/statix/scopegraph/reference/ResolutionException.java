package mb.statix.scopegraph.reference;

public class ResolutionException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResolutionException(String message) {
        this(message, null);
    }

    public ResolutionException(String message, Throwable cause) {
        super(message, cause, false, false);
    }

}