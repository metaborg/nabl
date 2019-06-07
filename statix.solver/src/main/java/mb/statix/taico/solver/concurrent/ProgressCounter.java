package mb.statix.taico.solver.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to track global progress that works with a simple atomic counter.
 * 
 * <p>An executor should call {@link #switchToPending()} when it knows it is going to do work in
 * the future, {@link #switchToWaiting()} when it switches to its waiting state and
 * {@link #switchToDone()} when it switches to its done state.
 * 
 * <p>Whenever no more progress can be made, {@code onFinished} is called.
 * The last executor that calls {@link #switchToDone()} or {@link #switchToWaiting()} will be the
 * one executing the {@code onFinished} function.
 */
public class ProgressCounter {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Runnable onFinished;
    
    /**
     * @param onFinished
     *      the runnable to execute once we are finished (done or stuck)
     */
    public ProgressCounter(Runnable onFinished) {
        this.onFinished = onFinished;
    }
    
    /**
     * This method should be called whenever an executor knows it is going to do more work in the
     * future (makes itself pending).
     */
    public void switchToPending() {
        counter.incrementAndGet();
    }
    
    /**
     * This method should be called whenever an executor switches to waiting.
     */
    public void switchToWaiting() {
        int value = counter.decrementAndGet();
        assert value >= 0 : "Counter value should never become negative!";
        if (value > 0) return;
        
        //We have hit zero, trigger the finished runnable
        onFinished.run();
    }
    
    /**
     * This method should be called whenever an executor switches to done.
     */
    public void switchToDone() {
        switchToWaiting();
    }
    
    /**
     * @return
     *      the number of executors that are currently working
     */
    public int getAmountWorking() {
        return counter.get();
    }
}
