package mb.statix.modular.util;

import static mb.statix.modular.util.TPrettyPrinter.printModule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.dot.DotPrinter;
import mb.statix.modular.module.IModule;
import mb.statix.modular.scopegraph.IMInternalScopeGraph;
import mb.statix.modular.solver.Context;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.log.NullDebugContext;

public class TDebug {
    /** The location where debug files will be written. */
    public static final String DEBUG_FILE_PATH = "/home/taico/spoofax";
    private static final String DEBUG_SCOPE_GRAPH_DOT_FILE = "yyyy.MM.dd 'at' HH_mm_ss";
    
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
    public static final boolean STORE_DEBUG = false;
    
    /** The interval at which progress is listed (in ms). */
    public static final long PROGRESS_TRACKER_INTERVAL = 30000;
    
    /** The level at which the coordinator should log its messages. */
    public static final Level COORDINATOR_LEVEL = Level.Info;
    
    /** If true, a summary is printed when the coordinator finishes. */
    public static final boolean COORDINATOR_SUMMARY = true;
    
    /** If true and COORDINATOR_SUMMARY is true, more details are printed when the coordinator finishes. */
    public static final boolean COORDINATOR_EXTENDED_SUMMARY = true;
    
    public static final IDebugContext DEV_NULL = new NullDebugContext();
    public static final IDebugContext DEV_OUT = new LoggerDebugContext(LoggerUtils.logger(TDebug.class), TOverrides.LOGLEVEL.equalsIgnoreCase("none") ? Level.Error : Level.parse(TOverrides.LOGLEVEL));
    
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
        IModule root = Context.context().getRootModule();
        outputScopeGraph(root.getScopeGraph(), "");
    }
    
    /**
     * Writes the scope graph to a file in {@value #DEBUG_FILE_PATH} ({@link #DEBUG_FILE_PATH}).
     * The file is named {@value #DEBUG_SCOPE_GRAPH_DOT_FILE} (@link {@link #DEBUG_SCOPE_GRAPH_DOT_FILE}),
     * which is a string formatted with a {@link SimpleDateFormat}.
     * 
     * @throws RuntimeException
     *      If writing fails for some reason.
     */
    public static void outputScopeGraph(IMInternalScopeGraph<Scope, ITerm, ITerm> graph, String nameSuffix) {
        DotPrinter printer = new DotPrinter(graph, true);
        String dotFile = printer.printDot();
        File folder = new File(DEBUG_FILE_PATH);
        if (!folder.exists() && !folder.mkdirs()) throw new RuntimeException("Unable to create debug folder " + DEBUG_FILE_PATH);
        
        SimpleDateFormat format = new SimpleDateFormat(DEBUG_SCOPE_GRAPH_DOT_FILE);
        String name = format.format(System.currentTimeMillis());
        File file = new File(folder, (sanitizeName(graph.getOwner().getId()) + "_" + nameSuffix + "_" + name + ".dot").replace(' ', '_'));
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(dotFile);
            bw.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static String sanitizeName(String name) {
        return name.replace("eclipse:///", "").replace("/", "").replace(":", "");
    }
    
    // --------------------------------------------------------------------------------------------
    // Debugging
    // --------------------------------------------------------------------------------------------
    public static void out(Object msg) {
        synchronized (System.err) {
            System.out.println(msg);
        }
    }
    
    public static void err(Object msg) {
        synchronized (System.out) {
            System.err.println(msg);
        }
    }
    
    public static void debugContext(Context context, boolean pretty) {
        System.out.println("Debug context " + System.identityHashCode(context));
        
        System.out.println("Modules: ");
        IModule root = context.getRootModule();
        printModuleHierarchy(root, 1);
        
        System.out.println("Unifiers: ");
        for (IModule module : context.getModules()) {
            System.out.println("| " + module + ":");
            System.out.println(context.getUnifier(module).print(pretty, 2));
        }
        
        System.out.println("Scope graphs: ");
        for (IModule module : context.getModules()) {
            System.out.println("| " + module + ":");
            System.out.println(context.getScopeGraph(module).print(pretty, 2));
        }
        
        System.out.println("Dependencies: ");
        for (IModule module : context.getModules()) {
            System.out.println("| " + module + ":");
            System.out.println(context.getDependencies(module).print(pretty, 2));
        }
    }
    
    private static void printModuleHierarchy(IModule module, int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) sb.append("| ");
        sb.append(printModule(module));
        System.out.println(sb.toString());
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, indent + 1);
        }
    }
}
