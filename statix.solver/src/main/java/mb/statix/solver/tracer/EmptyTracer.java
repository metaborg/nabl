package mb.statix.solver.tracer;

import mb.statix.solver.IState;

/**
 * An empty tracer.
 */
public final class EmptyTracer extends SolverTracer<EmptyTracer.Empty> {

    /**
     * An empty tracer result.
     */
    public static class Empty implements SolverTracer.IResult<Empty> {

        private static final long serialVersionUID = 42L;

        public static final Empty instance = new Empty();

        private Empty() { }

        @Override public Empty combine(Empty other) {
            return this;
        }

        public static Empty of() {
            return instance;
        }

        protected Object readResolve() {
            return instance;
        }
    }

    @Override public SolverTracer<Empty> subTracer() {
        return this;
    }

    @Override public Empty result(IState.Immutable state) {
        return Empty.instance;
    }

}
