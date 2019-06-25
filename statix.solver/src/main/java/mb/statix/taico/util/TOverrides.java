package mb.statix.taico.util;

public class TOverrides {
    /** Redirect STX_solve_constraint to MSTX_solve_constraint. */
    public static final boolean MODULES_OVERRIDE = true;
    /** If the log level should be overridden to the value below. */
    public static final boolean OVERRIDE_LOGLEVEL = true;
    /** The log level to use, has no effect if OVERRIDE_LOGLEVEL is false. */
    public static final String LOGLEVEL = "info"; //"debug" "none"
    /** If concurrency should be used. Uses the number of threads below. */
    public static final boolean CONCURRENT = false;
    /** The number of threads for concurrency. Has no effect if CONCURRENT is false. */
    public static final int THREADS = 2;
    
    /** If true, will cause the statix calls to fail in order to trigger a clean run. */
    public static final boolean CLEAN = false;
    
    public static String print() {
        return "Concurrent=" + (CONCURRENT ? THREADS : "false") +
                ", Loglevel=" + (OVERRIDE_LOGLEVEL ? LOGLEVEL : "not overridden");
    }
}
