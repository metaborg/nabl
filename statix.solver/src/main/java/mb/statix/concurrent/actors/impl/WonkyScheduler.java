package mb.statix.concurrent.actors.impl;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class WonkyScheduler implements IActorScheduler {

    private static final ILogger logger = LoggerUtils.logger(WonkyScheduler.class);

    private final ScheduledExecutorService executor;
    private final double preemptProbability;
    private final int scheduleDelayBoundMillis;
    private final Random rnd;

    public WonkyScheduler(int parallelism, double preemptProbability, int scheduleDelayBoundMillis) {
        this.executor = Executors.newScheduledThreadPool(parallelism);
        this.preemptProbability = preemptProbability;
        this.scheduleDelayBoundMillis = scheduleDelayBoundMillis;
        this.rnd = new Random();
    }

    @Override public void schedule(Runnable runnable, @SuppressWarnings("unused") int priority,
            AtomicReference<Runnable> taskRef) {
        final Task task = new Task(runnable);
        if(!taskRef.compareAndSet(null, task)) {
            logger.error("Actor {} already scheduled", runnable);
            throw new IllegalStateException("Actor " + runnable + " already scheduled.");
        }
        executor.schedule(task, rnd.nextInt(scheduleDelayBoundMillis), TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unused") @Override public void reschedule(Runnable oldTask, int newPriority,
            AtomicReference<Runnable> taskRef) {
    }

    @SuppressWarnings("unused") @Override public boolean preempt(int priority) {
        return rnd.nextDouble() < preemptProbability;
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