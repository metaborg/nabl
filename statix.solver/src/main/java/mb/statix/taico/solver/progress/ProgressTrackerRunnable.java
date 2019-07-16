package mb.statix.taico.solver.progress;

public class ProgressTrackerRunnable implements Runnable {
    public ProgressTracker tracker = new ProgressTracker();
    private long interval;
    private volatile Thread thread;
    private volatile boolean stop;
    
    public ProgressTrackerRunnable(long interval) {
        this.interval = interval;
    }
    
    public synchronized void start() {
        if (thread != null) return;
        
        stop = false;
        thread = new Thread(this, "ProgressTracker");
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
        try {
            while (!stop) {
                tracker.update();
                printResults();
                
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return;
                }
            }
        } finally {
            //Print one last time when stopping
            tracker.update();
            printResults();
        }
    }
    
    private void printResults() {
        System.err.println(tracker.toString());
    }

}
