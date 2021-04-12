package mb.scopegraph.pepm16;

public class StuckException extends Throwable {

    private static final long serialVersionUID = 1L;

    public StuckException() {
        super("stuck", null, false, false);
    }

}