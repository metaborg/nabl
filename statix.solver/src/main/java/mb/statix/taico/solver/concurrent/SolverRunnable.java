package mb.statix.taico.solver.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import mb.statix.taico.solver.ModuleSolver;

/**
 * Implementation for a runnable that represents the execution of a single solver.
 * This runnable will (re)schedule itself multiple times once more work needs to be done.
 * 
 * In each run, this solver runnable will execute its solver as far as possible. It then returns
 * to free the executor (yield). This solver is rescheduled with the executor whenever the
 * {@link #notifyOfWork()} method is called.
 */
public class SolverRunnable implements Runnable {
    private final ModuleSolver solver;
    private final Consumer<Runnable> schedule;
    
    private volatile boolean notified;
    private volatile boolean done;
    private volatile boolean working;
    private volatile boolean pending;
    
    private final AtomicInteger working2;
    
    
    /**
     * @param solver
     *      the solver to execute
     * @param working
     *      a counter for working solvers
     * @param schedule
     *      a consumer to schedule this solver runnable
     */
    public SolverRunnable(ModuleSolver solver, AtomicInteger working, Consumer<Runnable> schedule) {
        this.solver = solver;
        this.working2 = working;
        this.schedule = schedule;
    }

    @Override
    public void run() {
        synchronized (this) {
            working = true;
            pending = false;
        }
        working2.incrementAndGet();
        try {
            do {
                //Solve as far as possible. After each step, ignore the notifications since we are still solving anyways.
                do {
                    notified = false;
                } while (solver.solveStep());
                
                //If we are done, signal that we are no longer working and return
                if (done = solver.isDone()) {
                    setWorkingFalse();
                    return;
                }
                //Continue the process if we have been notified before the previous step completed
            } while (checkNotifiedAndResetWorking());
        } catch (InterruptedException e) {
            //We cannot guarantee consistent state any longer
            done = true;
            setWorkingFalse();
            throw new RuntimeException("FATAL: Solver executor of module " + solver.getOwner() + " was interrupted unexpectedly.", e);
        }
    }
    
    /**
     * <b>Note that this method is synchronized!</b>
     * 
     * <p>Sets working to false.
     */
    private synchronized void setWorkingFalse() {
        working = false;
    }
    
    /**
     * <b>Note that this method is synchronized!</b>
     * 
     * <p>Checks if we are notified. If we are, this method immediately returns true.
     * Otherwise, this method sets the working flag to false and returns false.
     * 
     * @return
     *      true if we have been notified, false otherwise
     */
    private synchronized boolean checkNotifiedAndResetWorking() {
        if (notified) return true;
        working = false;
        return false;
    }
    
    /**
     * Notifies this executor that there is more work to do.
     * 
     * <p>If the executor is currently not scheduled or running, it is scheduled by this method.
     */
    public void notifyOfWork() {
        if (done) return;
        
        synchronized (this) {
            if (done) return;
            
            notified = true;
            
            //If we are currently being executed or scheduled, we can ignore the notification
            if (working || pending) return;
            pending = true;
        }
        
        schedule();
    }
    
    /**
     * Schedules this solver runnable with its executor.
     */
    public void schedule() {
        try {
            schedule.accept(this);
        } catch (Throwable t) {
            throw new RuntimeException("FATAL: Unable to schedule solver for module " + solver.getOwner() + ". Giving up.", t);
        }
    }

    /**
     * @return
     *      true if this executor is done working (will not perform any work in the future), false
     *      otherwise
     */
    public boolean isDone() {
        return done;
    }
    
    /**
     * @return
     *      true if this executor is currently working, false otherwise
     */
    public boolean isWorking() {
        return working;
    }
    
    /**
     * @return
     *      true if this executor is currently waiting to be notified
     */
    public synchronized boolean isWaiting() {
        return !done && !pending && !working;
    }
}
