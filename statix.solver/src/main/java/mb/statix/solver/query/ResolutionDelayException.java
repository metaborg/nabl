package mb.statix.solver.query;

import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Delay;

public class ResolutionDelayException extends ResolutionException {

    private static final long serialVersionUID = 1L;

    private final Delay cause;

    public ResolutionDelayException(String message, Delay cause) {
        super(message, cause);
        this.cause = cause;
    }

    @Override public Delay getCause() {
        return cause;
    }

}