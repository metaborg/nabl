package mb.statix.scopegraph.reference;

public class StuckException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    public StuckException() {
        super("stuck");
    }

}