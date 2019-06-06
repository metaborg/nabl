package mb.statix.taico.util;

import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.log.NullDebugContext;

public class TDebug {
    public static final boolean QUERY_DELAY = false;
    /** If false, no debug messages will be created for the constraint solving in ModuleSolver. */
    public static final boolean CONSTRAINT_SOLVING = false;
    
    /** If true, debug info will be printed for the completeness. */
    public static final boolean COMPLETENESS = true;
    
    /** If true, completeness details will be made available (the constraints that cause the completeness to report false */
    public static final boolean COMPLETENESS_DETAILS = false;
    
    /** If true, queries will show more debug info. */
    public static final boolean QUERY_DEBUG = false;
    
    /** Debug info about delegation and events regarding the store. */
    public static final boolean STORE_DEBUG = true;
    
    public static final IDebugContext DEV_NULL = new NullDebugContext();
    public static final IDebugContext DEV_OUT = new LoggerDebugContext(LoggerUtils.logger(TDebug.class), Level.parse(TOverrides.LOGLEVEL));
}
