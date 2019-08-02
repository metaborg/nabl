package mb.statix.taico.solver.concurrent;

import java.util.function.Consumer;
import java.util.function.Supplier;

import mb.statix.taico.solver.ModuleSolver;
import mb.statix.taico.solver.Context;

/**
 * Implementation for a runnable that represents the execution of a single solver.
 * This runnable will reschedule itself once more work needs to be done.
 * 
 * <p>In each run, this solver runnable will execute its solver as far as possible. It then returns
 * to free the executor (yield). This solver is rescheduled whenever the {@link #notifyOfWork()}
 * method is called.
 * 
 * <p>Global progress is tracked with {@link ProgressCounter}.
 */
public class SolverRunnable implements Runnable {
    private final ModuleSolver solver;
    private final Consumer<Runnable> schedule;
    private final ProgressCounter progress;
    private final Consumer<ModuleSolver> onSuccess, onFailure;
    private final Supplier<Context> contextSupplier;

    private volatile boolean notified;
    private volatile boolean done;
    private volatile boolean working;
    private volatile boolean pending;
    
    /**
     * @param solver
     *      the solver to execute
     * @param schedule
     *      a consumer to schedule this solver runnable
     * @param progress
     *      a counter to check if we are still making any progress (globally)
     * @param contextSupplier
     *      the supplier of the context
     */
    public SolverRunnable(ModuleSolver solver, Consumer<Runnable> schedule, ProgressCounter progress, Supplier<Context> contextSupplier) {
        this(solver, schedule, progress, m -> {}, m -> {}, contextSupplier);
    }

    /**
     * @param solver
     *      the solver to execute
     * @param schedule
     *      a consumer to schedule this solver runnable
     * @param progress
     *      a counter to check if we are still making any progress (globally)
     * @param onSuccess
     *      a callback to execute on successful completion
     * @param onFailure
     *      a callback to execute on failing completion
     * @param contextSupplier
     *      the supplier of the context
     */
    public SolverRunnable(ModuleSolver solver, Consumer<Runnable> schedule, ProgressCounter progress,
            Consumer<ModuleSolver> onSuccess, Consumer<ModuleSolver> onFailure, Supplier<Context> contextSupplier) {
        this.solver = solver;
        this.schedule = schedule;
        this.progress = progress;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.contextSupplier = contextSupplier;
    }

    @Override
    public void run() {
        synchronized (this) {
            working = true;
            pending = false;
        }
        
        //Set the thread sensitive fields for the current thread
        Context.setThreadSensitiveContext(contextSupplier.get());
        Context.setCurrentModule(solver.getOwner());

        try {
            do {
                //Solve as far as possible. After each step, ignore the notifications since we are still solving anyways.
                do {
                    notified = false;
                } while (solver.solveStep());

                //If we are done, signal that we are no longer working and return
                if (done = (solver.isDone() || solver.hasFailed())) {
                    setWorkingFalse();
                    onCompletion();
                    return;
                }
                //Continue the process if we have been notified before the previous step completed
            } while (checkNotifiedAndResetWorking());
        } catch (InterruptedException e) {
            //We cannot guarantee consistent state any longer, so set done to true
            done = true;
            setWorkingFalse();
            throw new RuntimeException("FATAL: Solver executor of module " + solver.getOwner() + " was interrupted unexpectedly! Giving up.", e);
        } finally {
            if (done) {
                progress.switchToDone();
            } else {
                progress.switchToWaiting();
            }
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
     * Executes the proper completion functions.
     */
    private void onCompletion() {
        assert done : "The on completion method should not be called when we are not done!";
        if (solver.hasFailed()) {
            onFailure.accept(solver);
        } else {
            onSuccess.accept(solver);
        }
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
        progress.switchToPending();
        try {
            schedule.accept(this);
        } catch (Throwable t) {
            progress.switchToDone();
            throw new RuntimeException("FATAL: Unable to schedule solver for module " + solver.getOwner() + "! Giving up.", t);
        }
    }

    /**
     * @return
     *      true if this runnable is done working (will not perform any work in the future), false
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
    
    /**
     * Restarts this solver after it has become done once. This can be used for multi phase
     * solving.
     * <p>
     * This solver is only rescheduled if the solver currently reports that it is not done
     * nor failed.
     * 
     * @return
     *      true if the solver was restarted, false otherwise
     */
    public boolean restart() {
        boolean wasDone;
        synchronized (this) {
            if (working || pending) return false;
            
            wasDone = done;
            done = (solver.isDone() || solver.hasFailed());
            if (wasDone && done) {
                //If we were done and still are, just return false.
                return false;
            }
            
            //Otherwise, we will figure out when rescheduled
            pending = true;
        }
        
        schedule();
        return true;
    }
    
    /**
     * Recovers this runnable after a failure has occurred. This can be helpful e.g. when a mild
     * execution failure has occurred (e.g. interrupted).
     * <p>
     * This runnable is rescheduled if the solver it belongs to has not reached a finishing state
     * (done or failed).
     */
    public void recoverAfterFailure() {
        synchronized (this) {
            if (!done || working || pending) return;
            
            done = (solver.isDone() || solver.hasFailed());
            if (done) return;
            
            pending = true;
        }
        
        schedule();
    }
}
