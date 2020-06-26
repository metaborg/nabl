package mb.statix.actors;

public interface IActorMonitor {

    void suspend(IActorRef<?> actor);

    void resume(IActorRef<?> actor);

    public static final IActorMonitor NOOP = new IActorMonitor() {

        @Override public void suspend(IActorRef<?> actor) {
        }

        @Override public void resume(IActorRef<?> actor) {
        }

    };

}