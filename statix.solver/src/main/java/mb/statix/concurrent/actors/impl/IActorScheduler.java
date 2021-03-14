package mb.statix.concurrent.actors.impl;

import java.util.concurrent.atomic.AtomicReference;

public interface IActorScheduler {

    void schedule(Runnable runnable, int priority, AtomicReference<Runnable> taskRef);

    void reschedule(Runnable oldTask, int newPriority, AtomicReference<Runnable> taskRef);

    boolean preempt(int priority);

    void shutdown();

    void shutdownNow();

}