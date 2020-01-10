package mb.nabl2.solver.exceptions;

public class InterruptedDelayException extends DelayException {

    private static final long serialVersionUID = 42L;

    private final InterruptedException cause;

    public InterruptedDelayException(InterruptedException cause) {
        this.cause = cause;
    }

    @Override public InterruptedException getCause() {
        return cause;
    }

}