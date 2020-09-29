package mb.nabl2.scopegraph;

public class StuckException extends Throwable {

    private static final long serialVersionUID = 1L;

    public StuckException() {
        super("stuck", null, false, false);
    }

}