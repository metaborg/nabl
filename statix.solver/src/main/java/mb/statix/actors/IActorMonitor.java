package mb.statix.actors;

public interface IActorMonitor {

    void started(IActorRef<?> actor);

    void suspended(IActorRef<?> actor);

    void resumed(IActorRef<?> actor);

    void stopped(IActorRef<?> actor);

    public static final IActorMonitor NOOP = new IActorMonitor() {

        @Override public void started(IActorRef<?> actor) {
        }

        @Override public void suspended(IActorRef<?> actor) {
        }

        @Override public void resumed(IActorRef<?> actor) {
        }

        @Override public void stopped(IActorRef<?> actor) {
        }

    };

}