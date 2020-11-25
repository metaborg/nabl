package mb.statix.concurrent.actors.impl;

public class ActorRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ActorRuntimeException() {
    }

    public ActorRuntimeException(String message) {
        super(message);
    }

    public ActorRuntimeException(Throwable cause) {
        super(cause);
    }

    public ActorRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}