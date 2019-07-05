package mb.statix.taico.solver.progress;

public class ProgressTrackerRunnable implements Runnable {
    private ProgressTracker tracker = new ProgressTracker();
    private long interval;
    private volatile Thread thread;
    private volatile boolean stop;
    
    public ProgressTrackerRunnable(long interval) {
        this.interval = interval;
    }
    
    public synchronized void start() {
        if (thread != null) return;
        
        stop = false;
        thread = new Thread(this);
        thread.start();
    }
    
    public synchronized void stop() {
        if (thread == null || stop) return;
        
        stop = true;
        thread.interrupt();
        thread = null;
    }
    
    @Override
    public void run() {
        while (!stop) {
            tracker.update();
            printResults();
            
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
    
    private void printResults() {
        System.err.println(tracker.toString());
    }

}
