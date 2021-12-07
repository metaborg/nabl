package mb.scopegraph.patching;

public class InvalidPatchCompositionException extends RuntimeException {

    private static final long serialVersionUID = 42L;

    public InvalidPatchCompositionException(String message) {
        super(message);
    }

}
