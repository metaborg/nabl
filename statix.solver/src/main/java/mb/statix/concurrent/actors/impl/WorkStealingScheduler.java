package mb.statix.concurrent.actors.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class WorkStealingScheduler implements IActorScheduler {

    private static final ILogger logger = LoggerUtils.logger(WorkStealingScheduler.class);

    private final ExecutorService executor;

    public WorkStealingScheduler(int parallelism) {
        this.executor = Executors.newWorkStealingPool(parallelism);
    }

    @Override public void schedule(Actor<?> actor, int priority) {
        final ActorTask task = new ActorTask(actor);
        if(!actor.scheduledTask.compareAndSet(null, task)) {
            logger.error("Actor {} already scheduled", actor.id());
            throw new IllegalStateException("Actor " + actor.id() + " already scheduled.");
        }
        executor.execute(task);
    }

    @Override public void reschedule(Runnable oldTask, int newPriority) {
    }

    @Override public boolean preempt(int priority) {
        return false;
    }

    @Override public void shutdown() {
        executor.shutdown();
    }

    @Override public void shutdownNow() {
        executor.shutdownNow();
    }

    private class ActorTask implements Runnable {

        private final Actor<?> actor;
        private final AtomicBoolean active;

        ActorTask(Actor<?> runnable) {
            this.actor = runnable;
            this.active = new AtomicBoolean(true);
        }

        @Override public void run() {
            if(active.compareAndSet(true, false)) {
                actor.run();
            }
        }

    }

}