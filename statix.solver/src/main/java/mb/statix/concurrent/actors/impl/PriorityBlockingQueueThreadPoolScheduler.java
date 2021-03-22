package mb.statix.concurrent.actors.impl;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    @Override public boolean isActive() {
        return executor.getActiveCount() != 0 || !executor.getQueue().isEmpty();
    }

    @Override public void schedule(Runnable runnable, int priority, AtomicReference<Runnable> taskRef) {
        final Task task = new Task(runnable, priority);
        maxPriority = Math.max(maxPriority, priority);
        if(!taskRef.compareAndSet(null, task)) {
            logger.error("Actor {} already scheduled", runnable);
            throw new IllegalStateException("Actor " + runnable + " already scheduled.");
        }
        executor.execute(task);
    }

    @Override public void reschedule(Runnable oldTask, int newPriority, AtomicReference<Runnable> taskRef) {
        Task task = (Task) oldTask;
        if(task.priority * RESCHEDULE_FACTOR < newPriority) {
            if(task.active.compareAndSet(true, false)) {
                schedule(task.runnable, newPriority, taskRef);
            }
        }
    }

    @Override public boolean preempt(int priority) {
        // FIXME preempting when the executor is shutting down results in an exception in the actor
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
        final Task task = (Task) executorQueue.peek();
        if(task != null) {
            maxPriority = task.priority;
        } else {
            maxPriority = 0;
        }
    }

    private class Task implements Runnable, Comparable<Task> {

        private final Runnable runnable;
        private final int priority;
        private final AtomicBoolean active;

        Task(Runnable runnable, int priority) {
            this.runnable = runnable;
            this.priority = priority;
            this.active = new AtomicBoolean(true);
        }

        @Override public void run() {
            if(active.compareAndSet(true, false)) {
                updateMaxPriority();
                runnable.run();
            }
        }

        @Override public int compareTo(Task o) {
            return o.priority - priority;
        }

    }

}