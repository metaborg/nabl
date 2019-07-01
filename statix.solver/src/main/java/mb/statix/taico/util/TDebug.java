package mb.statix.taico.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.taico.dot.DotPrinter;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.SolverContext;

public class TDebug {
    /** The location where debug files will be written. */
    public static final String DEBUG_FILE_PATH = "/home/taico/spoofax";
    private static final String DEBUG_SCOPE_GRAPH_DOT_FILE = "yyyy.MM.dd 'at' HH:mm:ss";
    
    public static final boolean QUERY_DELAY = false;
    /** If false, no debug messages will be created for the constraint solving in ModuleSolver. */
    public static final boolean CONSTRAINT_SOLVING = false;
    
    /** If true, debug info will be printed for the completeness. */
    public static final boolean COMPLETENESS = false;
    
    /** If true, completeness details will be made available (the constraints that cause the completeness to report false */
    public static final boolean COMPLETENESS_DETAILS = false;
    
    /** If true, queries will show more debug info. */
    public static final boolean QUERY_DEBUG = false;
    
    /** Debug info about delegation and events regarding the store. */
    public static final boolean STORE_DEBUG = true;
    
    public static final IDebugContext DEV_NULL = new NullDebugContext();
    public static final IDebugContext DEV_OUT = new LoggerDebugContext(LoggerUtils.logger(TDebug.class), Level.parse(TOverrides.LOGLEVEL));
    
    public static String print() {
        return "QUERY_DELAY=" + QUERY_DELAY +
                ", CONSTRAINT_SOLVING=" + CONSTRAINT_SOLVING +
                ", COMPLETENESS=" + COMPLETENESS +
                ", COMPLETENESS_DETAILS=" + COMPLETENESS_DETAILS +
                ", QUERY_DEBUG=" + QUERY_DEBUG +
                ", STORE_DEBUG=" + STORE_DEBUG;
    }
    
    /**
     * Writes the scope graph to a file in {@value #DEBUG_FILE_PATH} ({@link #DEBUG_FILE_PATH}).
     * The file is named {@value #DEBUG_SCOPE_GRAPH_DOT_FILE} (@link {@link #DEBUG_SCOPE_GRAPH_DOT_FILE}),
     * which is a string formatted with a {@link SimpleDateFormat}.
     * 
     * @throws RuntimeException
     *      If writing fails for some reason.
     */
    public static void outputScopeGraph() {
        IModule root = SolverContext.context().getRootModule();
        MSolverResult result = MSolverResult.of(root.getCurrentState(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
        DotPrinter printer = new DotPrinter(result, null);
        String dotFile = printer.printDot();
        File folder = new File(DEBUG_FILE_PATH);
        if (!folder.exists() && !folder.mkdirs()) throw new RuntimeException("Unable to create debug folder " + DEBUG_FILE_PATH);
        
        SimpleDateFormat format = new SimpleDateFormat(DEBUG_SCOPE_GRAPH_DOT_FILE);
        String name = format.format(System.currentTimeMillis());
        File file = new File(folder, root.getId() + " " + name + ".dot");
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(dotFile);
            bw.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
