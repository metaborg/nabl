package mb.statix.concurrent.actors.impl;

public class ActorException extends Exception {

    private static final long serialVersionUID = 1L;

    protected ActorException() {
    }

    public ActorException(String message) {
        super(message);
    }

    public ActorException(Throwable cause) {
        super(cause);
    }

    public ActorException(String message, Throwable cause) {
        super(message, cause);
    }

}