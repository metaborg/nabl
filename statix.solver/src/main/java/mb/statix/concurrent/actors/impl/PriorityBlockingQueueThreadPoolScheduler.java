package mb.statix.concurrent.actors.impl;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class PriorityBlockingQueueThreadPoolScheduler implements IActorScheduler {

    private static final ILogger logger = LoggerUtils.logger(PriorityBlockingQueueThreadPoolScheduler.class);

    private static final int PREEMPT_FACTOR = 3;
    private static final int RESCHEDULE_FACTOR = 7;

    private final PriorityBlockingQueue<Runnable> executorQueue;
    private final ThreadPoolExecutor executor;

    private volatile int maxPriority = 0;

    // FIXME Rebuild queue when #rescheduled > #scheduled

    public PriorityBlockingQueueThreadPoolScheduler(int parallelism) {
        this.executorQueue = new PriorityBlockingQueue<>();
        this.executor = new ThreadPoolExecutor(parallelism, parallelism, 60L, TimeUnit.SECONDS, executorQueue);
    }

    @Override public void schedule(Actor<?> actor, int priority) {
        final ActorTask task = new ActorTask(actor, priority);
        maxPriority = Math.max(maxPriority, priority);
        if(!actor.scheduledTask.compareAndSet(null, task)) {
            logger.error("Actor {} already scheduled", actor.id());
            throw new IllegalStateException("Actor " + actor.id() + " already scheduled.");
        }
        executor.execute(task);
    }

    @Override public void reschedule(Runnable oldTask, int newPriority) {
        ActorTask task = (ActorTask) oldTask;
        if(task.priority * RESCHEDULE_FACTOR < newPriority) {
            if(task.active.compareAndSet(true, false)) {
                schedule(task.actor, newPriority);
            }
        }
    }

    @Override public boolean preempt(int priority) {
        return priority * PREEMPT_FACTOR < maxPriority;
    }

    @Override public void shutdown() {
        executor.shutdown();
    }

    @Override public void shutdownNow() {
        executor.shutdownNow();
    }

    private void updateMaxPriority() {
        // FIXME Top task may be inactive, possibly setting the priority too high.
        final ActorTask task = (ActorTask) executorQueue.peek();
        if(task != null) {
            maxPriority = task.priority;
        } else {
            maxPriority = 0;
        }
    }

    private class ActorTask implements Runnable, Comparable<ActorTask> {

        private final Actor<?> actor;
        private final int priority;
        private final AtomicBoolean active;

        ActorTask(Actor<?> runnable, int priority) {
            this.actor = runnable;
            this.priority = priority;
            this.active = new AtomicBoolean(true);
        }

        @Override public void run() {
            if(active.compareAndSet(true, false)) {
                updateMaxPriority();
                actor.run();
            }
        }

        @Override public int compareTo(ActorTask o) {
            return o.priority - priority;
        }

    }

}