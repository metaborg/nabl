package mb.statix.concurrent.actors.impl;

public class ActorStoppedException extends ActorRuntimeException {

    private static final long serialVersionUID = 1L;

    public ActorStoppedException() {
        super();
    }

    public ActorStoppedException(String message) {
        super(message);
    }

    public ActorStoppedException(Throwable cause) {
        super(cause);
    }

    public ActorStoppedException(String message, Throwable cause) {
        super(message, cause);
    }

}