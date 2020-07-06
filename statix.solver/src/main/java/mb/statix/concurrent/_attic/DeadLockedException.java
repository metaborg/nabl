package mb.statix.concurrent._attic;

public class DeadLockedException extends Exception {

    private static final long serialVersionUID = 1L;

    public DeadLockedException() {
        this("deadlocked");
    }

    public DeadLockedException(String message) {
        super(message);
    }

}