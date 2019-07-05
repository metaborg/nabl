package mb.statix.taico.util;

public class TOverrides {
    /** Redirect STX_solve_constraint to MSTX_solve_constraint. */
    public static boolean MODULES_OVERRIDE = true;
    /** If the log level should be overridden to the value below. */
    public static boolean OVERRIDE_LOGLEVEL = true;
    /** The log level to use, has no effect if OVERRIDE_LOGLEVEL is false. */
    public static String LOGLEVEL = "info"; //"debug" "none"
    /** If concurrency should be used. Uses the number of threads below. */
    public static boolean CONCURRENT = true;
    /** The number of threads for concurrency. Has no effect if CONCURRENT is false. */
    public static int THREADS = 2;
    
    /**
     * If true, the observer mechanism is used for own critical edges.
     * Otherwise, the"just redo whenever the critical edge MIGHT have changed" variant is used. 
     */
    public static boolean USE_OBSERVER_MECHANISM_FOR_SELF = true;
    
    /** If true, will cause the statix calls to fail in order to trigger a clean run. */
    public static boolean CLEAN = false;
    
    public static String print() {
        return "Concurrent=" + (CONCURRENT ? THREADS : "false") +
                ", Loglevel=" + (OVERRIDE_LOGLEVEL ? LOGLEVEL : "not overridden") +
                ", UseObserverMechanismSelf=" + USE_OBSERVER_MECHANISM_FOR_SELF;
    }
}
