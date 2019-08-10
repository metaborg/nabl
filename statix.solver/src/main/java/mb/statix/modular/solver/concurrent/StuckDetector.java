package mb.statix.modular.solver.concurrent;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.metaborg.util.log.LoggerUtils;

import mb.statix.modular.module.IModule;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.progress.ProgressTracker;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.util.TTimings;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;

public class StuckDetector implements Runnable {
    private ConcurrentSolverCoordinator coordinator;
    private ProgressTracker tracker;
    private int cActive, cDelayed, mComplete, mFailed;
    private int same;
    private volatile Thread thread;
    private volatile boolean stop;
    
    public StuckDetector(ConcurrentSolverCoordinator coordinator, ProgressTracker tracker) {
        this.coordinator = coordinator;
        this.tracker = tracker;
    }
    
    public synchronized void start() {
        if (thread != null) return;
        stop = false;
        thread = new Thread(this, "StuckDetector");
        thread.start();
    }
    
    public synchronized void stop() {
        if (thread == null) return;
        stop = true;
        thread.interrupt();
        thread = null;
    }
    
    @Override
    public void run() {
        while (!stop) {
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                if (stop) return;
            }
            
            int ca, cd, mc, mf;
            if ((ca = tracker.constraintsActive) == cActive &
                (cd = tracker.constraintsDelayed) == cDelayed &
                (mc = tracker.modulesComplete) == mComplete &
                (mf = tracker.modulesFailed) == mFailed) {
                same++;
            } else {
                same = 0;
                cActive = ca;
                cDelayed = cd;
                mComplete = mc;
                mFailed = mf;
            }
            
            if (same == 4) {
                System.err.println("Detected no progress for 1 minute, printing/saving results...");
                printSolverState();
            }
        }
    }
    
    /**
     * Prints the state of the solver, reporting solvers that are not responding.
     * This also writes the timings so far to csv.
     */
    public void printSolverState() {
        TTimings.startPhase("Stuck Detector");
        IDebugContext debug = new LoggerDebugContext(LoggerUtils.logger(StuckDetector.class));
        coordinator.logDebugInfo(debug);
        
        debug._info("Reported remaining solvers 'working': {}", coordinator.getProgressCounter().getAmountWorking());
        
        final Set<ModuleSolver> solvers = new HashSet<>(coordinator.getSolvers());
        coordinator.getResults().keySet().stream().map(IModule::getCurrentState).map(IMState::solver).forEach(m -> solvers.remove(m));
        
        IDebugContext sub = debug.subContext();
        
        debug._info("Solvers that are not responding:");
        for (ModuleSolver solver : solvers) {
            sub._info("{}, delayed:", solver.getOwner().getId());
            
            IDebugContext s = sub.subContext();
            solver.getStore().delayed().entrySet().stream().forEach(e -> s._info("on {}: {}", e.getValue(), e.getKey()));
        }
        
        //Write the timings
        TTimings.endPhase("Stuck Detector");
        TTimings.serialize(new File(TTimings.getFile().toString().replace(".csv", "_aborted.csv")));
    }
    
}
