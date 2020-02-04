package mb.nabl2.solver.exceptions;

public class SolverException extends Exception {

    private static final long serialVersionUID = 42L;

    public SolverException(String message) {
        super(message);
    }

    public SolverException(Throwable cause) {
        super(cause);
    }

    public SolverException(String message, Throwable cause) {
        super(message, cause);
    }

}