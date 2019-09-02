package mb.statix.solver;

public abstract class SolverException extends Throwable {

    private static final long serialVersionUID = 1L;

    public SolverException(String msg) {
        super(msg, null, false, false);
    }

    public abstract void rethrow() throws InterruptedException;

    public static class SolverInterrupted extends SolverException {

        private static final long serialVersionUID = 1L;

        private final InterruptedException ex;

        public SolverInterrupted(InterruptedException ex) {
            super("interrupted");
            this.ex = ex;
        }

        @Override public void rethrow() throws InterruptedException {
            throw ex;
        }

    }

}