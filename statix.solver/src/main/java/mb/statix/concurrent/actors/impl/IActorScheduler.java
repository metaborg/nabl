package mb.statix.concurrent.actors.impl;

public interface IActorScheduler {

    void schedule(Actor<?> actor, int priority);

    void reschedule(Runnable oldTask, int newPriority);

    boolean preempt(int priority);

    void shutdown();

    void shutdownNow();

}