package mb.statix.solver.concurrent;

public class DeadLockedException extends Exception {

    private static final long serialVersionUID = 1L;

    public DeadLockedException() {
        this("deadlocked");
    }

    public DeadLockedException(String message) {
        super(message);
    }

}