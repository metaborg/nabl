package mb.statix.concurrent.actors.impl;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class ThreadPoolScheduler implements IActorScheduler {

    private static final ILogger logger = LoggerUtils.logger(ThreadPoolScheduler.class);

    private final int parallelism;
    private final ThreadPoolExecutor executor;

    public ThreadPoolScheduler(int parallelism) {
        this.parallelism = parallelism;
        this.executor =
                new ThreadPoolExecutor(parallelism, parallelism, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    }

    @Override public int parallelism() {
        return parallelism;
    }

    @Override public boolean isActive() {
        return executor.getActiveCount() != 0 || !executor.getQueue().isEmpty();
    }

    @Override public void schedule(Runnable runnable, @SuppressWarnings("unused") int priority,
            AtomicReference<Runnable> taskRef) {
        final Task task = new Task(runnable);
        if(!taskRef.compareAndSet(null, task)) {
            logger.error("Actor {} already scheduled", runnable);
            throw new IllegalStateException("Actor " + runnable + " already scheduled.");
        }
        executor.execute(task);
    }

    @SuppressWarnings("unused") @Override public void reschedule(Runnable oldTask, int newPriority,
            AtomicReference<Runnable> taskRef) {
    }

    @SuppressWarnings("unused") @Override public boolean preempt(int priority) {
        return false;
    }

    @Override public void shutdown() {
        executor.shutdown();
    }

    @Override public void shutdownNow() {
        executor.shutdownNow();
    }

    private class Task implements Runnable {

        private final Runnable runnable;
        private final AtomicBoolean active;

        Task(Runnable runnable) {
            this.runnable = runnable;
            this.active = new AtomicBoolean(true);
        }

        @Override public void run() {
            if(active.compareAndSet(true, false)) {
                runnable.run();
            }
        }

    }

}