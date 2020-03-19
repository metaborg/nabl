package mb.statix.solver.persistent;

abstract class GreedySolverException extends Exception {

    private static final long serialVersionUID = 1L;

    public GreedySolverException(Throwable cause) {
        super("internal exception", cause, false, false);
    }

    public abstract void rethrow() throws InterruptedException;

    static GreedySolverException of(InterruptedException cause) {
        return new Interrupted(cause);
    }

    private static class Interrupted extends GreedySolverException {

        private static final long serialVersionUID = 1L;

        private final InterruptedException cause;

        public Interrupted(InterruptedException cause) {
            super(cause);
            this.cause = cause;
        }

        @Override public void rethrow() throws InterruptedException {
            throw cause;
        }

    }

}