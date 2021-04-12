package mb.scopegraph.oopsla20.reference;

public class StuckException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    public StuckException() {
        super("stuck");
    }

}