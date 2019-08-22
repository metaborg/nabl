package statix.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResults;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.terms.io.TAFTermReader;

import mb.statix.modular.dot.DotPrinter;
import mb.statix.modular.module.IModule;
import mb.statix.modular.solver.Context;
import mb.statix.modular.util.TDebug;
import mb.statix.modular.util.TOptimizations;
import mb.statix.modular.util.TOverrides;
import mb.statix.modular.util.TPrettyPrinter;
import mb.statix.modular.util.TTimings;
import mb.statix.modular.util.TTimings.PhaseDetails;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.RunLast;
import picocli.CommandLine.Spec;
import statix.cli.incremental.IncrementalChangeGenerator;
import statix.cli.incremental.changes.IncrementalChange;
import statix.cli.incremental.changes.ast.RemoveClass;
import statix.cli.incremental.changes.ast.RemoveFile;
import statix.cli.incremental.changes.ast.RemoveMethod;
import statix.cli.incremental.changes.ast.RenameClass;
import statix.cli.incremental.changes.ast.RenameMethod;
import statix.cli.incremental.changes.other.AddClass;
import statix.cli.incremental.changes.source.AddMethod;

@Command(name = "java -jar statix.jar", description = "Type check and evaluate Statix files.", separator = "=")
public class MStatix implements Callable<Void> {
    private static final ILogger logger = LoggerUtils.logger(MStatix.class);
    public static final Spoofax S;
    
    static {
        Spoofax spoofax;
        try {
            spoofax = new Spoofax();
        } catch (MetaborgException e) {
            spoofax = null;
        }
        
        S = spoofax;
        
        forceInit(AddMethod.class, RenameMethod.class, RemoveMethod.class);
        forceInit(RemoveFile.class);
        forceInit(AddClass.class, RenameClass.class, RemoveClass.class);
    }
    
    @Spec private CommandSpec spec;
    @Option(names = { "-h", "--help" }, description = "show usage help", usageHelp = true) private boolean usageHelp;
    @Option(names = { "-e", "--ext" }, required = true, description = "file extension") private String extension;
    @Option(names = { "-l", "--language" }, required = true, description = "language under test") private String language;
    @Option(names = { "-o", "--output" }, required = true, description = "location for csv output") private String output;
    @Option(names = { "-g", "--outputgraph" }, description = "dot output for scope graph") private String outputGraph;
    @Option(names = { "-d", "--outputdiff" }, description = "diff output for incremental") private String outputDiff;
    @Option(names = { "--outputindividualgraphs" }, description = "dot output for individual scope graphs") private boolean outputIndividualGraphs;
    @Option(names = { "-s", "--seed" }, description = "seed for random changes") private long seed;
    @Option(names = { "-split", "--splitmodules" }, description = "if split modules should be enabled") private boolean splitModules;
    @Option(names = { "-f", "--folder" }, description = "the output folder where change files are stored") private String outputFolder;
    @Option(
            names = { "-m", "--mode" }, 
            required = true,
            description = "0 = clean OR incremental, 1 = clean followed by incremental, 2 = incremental followed by clean")
    private int mode;
    
    @Option(names = { "-w", "--warmup" }, description = "the location of the warmup folder") private String warmupFolder;
    @Option(names = { "-wc", "--warmupCount" }, description = "the amount of warmup runs") private int warmupCount;
    
    @Option(names = { "-cin", "--contextin" }, description = "file to load context") private String contextIn;
    @Option(names = { "-cout", "--contextout" }, description = "file to save context") private String contextOut;
    
    @Option(names = { "-c", "--count" }, description = "amount of runs", defaultValue = "1") private int count;
    @Option(names = { "-t", "--threads" }, description = "amount of threads to use", defaultValue = "1") private int threads;
    @Option(names = { "--observerself" }, description = "observer mechanism for own module", defaultValue = "true") private boolean observer;
//    @Option(names = { "--scopehash" }, description = "use scope hashes", defaultValue = "true") private boolean scopehash;
    @Option(
            names = { "--syncscopegraphs" },
            description = "use synchronized (1), locks (2) or combined locking (3) for locking scope graphs",
            defaultValue = "1")
    private int syncSgs;
    
    @Parameters(
            paramLabel = "FOLDER",
            index = "0",
            description = "the folder containing the files to anlyze")
    private String folder;
    
    @Parameters(
            paramLabel = "CHANGE",
            index = "1..*",
            description = "the incremental changes to apply. e.g. method:add, class:*, *:remove, *:*")
    private List<String> changes;
    
    //---------------------------------------------------------------------------------------------
    
    private StatixData data;
    private StatixParse parse;
    private StatixAnalyze analyze;
    private TestRandomness random;
    private List<IncrementalChange> ichanges;
    
    @Override public Void call() throws MetaborgException, IOException {
        init();
        
        //Identify the files in the project
        List<File> files;
        try {
            files = identifyFiles(folder);
        } catch (IOException e) {
            throw new MetaborgException("Unable to identify files to parse: ", e);
        }
        
        try {
            warmup();
            switch (mode) {
                case 0: 
                    cleanOrIncrementalAnalysis(files);
                    break;
                case 1:
                    cleanAndIncrementalAnalysis(files);
                    break;
                case 2:
                    incrementalAndCleanAnalysis(files);
                    break;
                default:
                    throw new MetaborgException("Illegal mode: " + mode);
            }
            
        } finally {
            exit();
        }
        return null;
    }

    /**
     * Performs the same analysis multiple times. The analysis performed is a clean run if no
     * changes are supplied or an incremental analysis otherwise.
     * <p>
     * - Each run works on the same loaded context<br>
     * - The context is loaded from file each time (if applicable)<br>
     * - The seed is reset each run, so the same incremental change is applied each time<br>
     * 
     * @param files
     * @throws MetaborgException
     */
    private void cleanOrIncrementalAnalysis(List<File> files) throws MetaborgException {
        boolean clean = ichanges.isEmpty();
        
        for (int run = 0; run < count; run++) {
            startTimedRun(clean);

            //Load the context from scratch each time
            loadContext("" + run);
            if (clean) {
                cleanAnalysis(files, run + "c");
            } else {
                incrementalAnalysis(files, run + "i");
            }
            endTimedRun(clean);
            
            //Only save the context once
            contextOut = null;
            
            //Be sure to unload the context
            unloadContext();
            random = createRandom();
            writeTimings();
        }
    }
    
    /**
     * Performs the same pair of analyses multiple times. The first analysis is always a clean run
     * and the second analysis is an incremental run.
     * <p>
     * - Each run works on the same initial input<br>
     * - The context is unloaded after the incremental run<br>
     * - Each run uses the same seed to ensure that the same change occurs each time
     * 
     * @param files
     * @throws MetaborgException
     */
    private void cleanAndIncrementalAnalysis(List<File> files) throws MetaborgException {
        if (ichanges.isEmpty()) throw new IllegalArgumentException("You need to specify changes!");

        for (int run = 0; run < count; run++) {
            startTimedRun(true);
            cleanAnalysis(files, run + "c");
            endTimedRun(true);
            
            //Only save the output once
            contextOut = null;
            
            startTimedRun(false);
            incrementalAnalysis(files, run + "i");
            endTimedRun(false);
            
            //Unload the context after each run to free memory
            unloadContext();
            
            //Reset the random to the same seed
            random = createRandom();
            writeTimings();
        }
    }
    
    /**
     * For each run, performs an incremental analysis and then a clean analysis on the same input
     * as the incremental one. This can be used to measure the difference.
     * <p>
     * - The context is loaded for each run<br>
     * - The seed is restored each run, to ensure the same change is applied each time<br>
     * 
     * @param files
     * @throws MetaborgException
     */
    private void incrementalAndCleanAnalysis(List<File> files) throws MetaborgException {
        if (ichanges.isEmpty()) throw new IllegalArgumentException("You need to specify changes!");
        
        for (int run = 0; run < count; run++) {
            loadContext(run + "i");
            startTimedRun(false);
            ISpoofaxParseUnit modified = incrementalAnalysis(files, run + "i");
            endTimedRun(false);
            
            //Only save the context once
            contextOut = null;
            unloadContext();
            random = createRandom();
            loadContext(run + "c");
            
            startTimedRun(true);
            modifiedCleanAnalysis(files, modified, run + "c");
            endTimedRun(true);
            
            unloadContext();
            writeTimings();
        }
    }
    
    //---------------------------------------------------------------------------------------------
    //Analyses
    //---------------------------------------------------------------------------------------------
    
    /**
     * Executes the warmup.
     * 
     * @throws MetaborgException
     */
    public void warmup() throws MetaborgException {
        if (warmupFolder == null || warmupCount == 0) {
            System.err.println("Skipping warmup");
            return;
        }
        
        List<File> files;
        try {
            files = identifyFiles(warmupFolder);
        } catch (IOException e) {
            throw new MetaborgException("Unable to identify files to parse for warmup: ", e);
        }
        
        int timing = TTimings.startNewRun();
        TTimings.fixRun();
        TTimings.startPhase("warmup");
        for (int run = 0; run < warmupCount; run++) {
            
            TTimings.startPhase("warmup " + run);
            List<ISpoofaxParseUnit> parsed = parse.parseFiles(files);
            analyze.cleanAnalysis(parsed, false);
            TTimings.endPhase("warmup " + run);
            unloadContext();
        }
        TTimings.endPhase("warmup");
        
        Iterator<String> it = TTimings.results.get(timing).keySet().iterator();
        while (it.hasNext()) {
            String s = it.next();
            if (!s.startsWith("warmup")) it.remove();
        }
        TTimings.unfixRun();
    }
    
    /**
     * Performs a clean analysis.
     * 
     * @throws MetaborgException
     */
    public void cleanAnalysis(List<File> files, String run) throws MetaborgException {
        logger.info("Starting clean analysis");
        
        resetDiffOutput(run);
        
        TTimings.startPhase("parsing", "Count: " + files.size());
        List<ISpoofaxParseUnit> parsed = parse.parseFiles(files);
        TTimings.endPhase("parsing");
        
        TTimings.startPhase("analysis full");
        ISpoofaxAnalyzeResults results = analyze.cleanAnalysis(parsed, true);
        TTimings.endPhase("analysis full");
        
        writeFailedResults(results, run);
        
        saveContext();
        writeScopeGraph(run);
    }
    
    /**
     * Performs a clean analysis, but with the given parse units changed
     * 
     * @param files
     *      the files
     * @param changed
     *      the changed parse unit
     * @throws MetaborgException
     */
    public void modifiedCleanAnalysis(List<File> files, ISpoofaxParseUnit changed, String run) throws MetaborgException {
        logger.info("Starting modified clean analysis");
        
        resetDiffOutput(run);
        
        TTimings.startPhase("parsing", "Count: " + files.size());
        List<ISpoofaxParseUnit> parsed = parse.parseFiles(files);
        TTimings.endPhase("parsing");
        
        ListIterator<ISpoofaxParseUnit> lit = parsed.listIterator();
        while (lit.hasNext()) {
            ISpoofaxParseUnit original = lit.next();
            if (!original.source().equals(changed.source())) continue;
            
            lit.set(changed);
            break;
        }

        TTimings.startPhase("analysis full");
        ISpoofaxAnalyzeResults results = analyze.cleanAnalysis(parsed, true);
        TTimings.endPhase("analysis full");
        
        writeFailedResults(results, run);
        
        saveContext();
        writeScopeGraph(run);
    }
    
    /**
     * Performs an incremental analysis.
     * @return 
     * 
     * @throws MetaborgException
     */
    public ISpoofaxParseUnit incrementalAnalysis(final List<File> files, String run) throws MetaborgException {
        logger.info("Starting incremental analysis");
        
        resetDiffOutput(run);
        
        //First we need to select the change to apply
        TTimings.startPhase("identifying files");
        
        TTimings.endPhase("identifying files");
        
        TTimings.startPhase("incremental change + parse");
        IncrementalChangeGenerator generator = new IncrementalChangeGenerator(data, parse, random, files, ichanges);
        ISpoofaxParseUnit modifiedUnit = generator.tryApply(this, run);
        TTimings.endPhase("incremental change + parse");
        
        writeChangeToFile(modifiedUnit, run);
        
        logger.info("Modified file: " + modifiedUnit.source());
        
        TTimings.startPhase("analysis full");
        ISpoofaxAnalyzeResults results = analyze.analyzeAll(Iterables2.singleton(modifiedUnit), true);
        TTimings.endPhase("analysis full");

        writeFailedResults(results, run);
        
        saveContext();
        writeScopeGraph(run);
        return modifiedUnit;
    }
    
    //---------------------------------------------------------------------------------------------
    //Initialization and finalization
    //---------------------------------------------------------------------------------------------
    
    public void init() throws MetaborgException {
        data = new StatixData(S, System.out);
        parse = new StatixParse(S, data.getLanguage(language), data.getMessagePrinter());
        analyze = new StatixAnalyze(S, data.createContext(language), data.getMessagePrinter());
        random = createRandom();
        ichanges = IncrementalChange.parse(changes == null ? (changes = new ArrayList<>()) : changes);
        
        //Set the concurrency
        TOverrides.CONCURRENT = threads > 1;
        TOverrides.THREADS = threads;
        
        //Disable debug logging
        TOverrides.OVERRIDE_LOGLEVEL = true;
        TOverrides.LOGLEVEL = "none";
        
        //Disable the modular override for when we want to benchmark the original solver
        TOverrides.MODULES_OVERRIDE = false;
        
        //Set sync scopegraphs mechanism
        TOverrides.SYNC_SCOPEGRAPHS = syncSgs;
        
        TOverrides.SPLIT_MODULES = splitModules;
        
        //Set optimizations
        TOptimizations.USE_OBSERVER_MECHANISM_FOR_SELF = observer;
//        TOptimizations.SCOPE_HASHES = scopehash;
        
        //Clean the other debug settings. These will be set elsewhere if necessary for output
        TOverrides.OUTPUT_DIFF = false;
        TOverrides.OUTPUT_SCOPE_GRAPH_MULTI = false;
        TOverrides.OUTPUT_SCOPE_GRAPH_SINGLE = false;
    }
    
    public void exit() {
        writeTimings();
    }
    
    //------------------------------------writeExpectedUsages---------------------------------------------------------
    //Other
    //---------------------------------------------------------------------------------------------
    
    public TestRandomness createRandom() {
        return new TestRandomness(seed == 0 ? (seed = System.currentTimeMillis()) : seed);
    }
    
    //---------------------------------------------------------------------------------------------
    
    /**
     * Identifies all the files that should be parsed.
     * 
     * @param folder
     *      the folder to look in
     * 
     * @return
     *      the files
     * 
     * @throws IOException
     *      If reading the contents of the directories encounters an error.
     */
    public List<File> identifyFiles(String folder) throws IOException {
        logger.info("Identifying files in " + folder);
        File root = new File(folder);
        if (!root.exists()) throw new IllegalArgumentException("The specified folder does not exist!");
        
        Stream<Path> stream = Files.walk(root.toPath()).filter(Files::isRegularFile);
        if (extension != null && !extension.isEmpty()) {
            stream = stream.filter(p -> p.toFile().getName().endsWith(extension));
        }
        
        List<File> tbr = stream.map(Path::toFile).collect(Collectors.toList());
        logger.info("Identified " + tbr.size() + " files in the project");
        return tbr;
    }
    
    public void startTimedRun(boolean clean) {
        TTimings.startNewRun();
        TTimings.fixRun();
        String phase = clean ? "clean run" : "incremental run";
        TTimings.startPhase(phase,
                "Project: " + new File(folder).getAbsolutePath(),
                "Language: " + language,
                "Mode: " + mode,
                "Clean: " + clean,
                "Changes: " + changes,
                "Seed: " + random.getSeed(),
                "Concurrent: " + (threads == 1 ? "false" : "" + threads),
                "Sync: " + syncSgs,
                "ObserverSelf: " + observer);
    }
    
    public void endTimedRun(boolean clean) {
        String phase = clean ? "clean run" : "incremental run";
        TTimings.endPhase(phase);
        TTimings.unfixRun();
    }
    
    //---------------------------------------------------------------------------------------------
    //Auxilary output (Scope graph, diff, changes, timings)
    //---------------------------------------------------------------------------------------------
    
    /**
     * Writes the scope graph to a file.
     */
    public void writeScopeGraph(String run) {
        writeIndividualScopeGraphs(run);
        if (outputGraph == null || outputGraph.isEmpty() || Context.context() == null) return;
        TPrettyPrinter.resetScopeNumbers();
        TTimings.startPhase("printing scope graph");
        DotPrinter printer = new DotPrinter(null);
        
        File outputFile = new File(outputGraph + "_" + run + ".dot");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(printer.printDot());
        } catch (IOException ex) {
            System.err.println("Unable to write scope graph output file to " + outputGraph);
            ex.printStackTrace();
        }
        TTimings.endPhase("printing scope graph");
    }
    
    /**
     * Writes each scope graph of a top level module to file.
     */
    public void writeIndividualScopeGraphs(String run) {
        if (!outputIndividualGraphs || Context.context() == null) return;
        
        TTimings.startPhase("printing individual scope graphs");
        
        for (IModule module : Context.context().getModulesOnLevel(1, false).values()) {
            DotPrinter printer = new DotPrinter(module.getScopeGraph(), true);
            
            File outputFile = new File(outputGraph + "_" + run + "_" + module.getId().replaceAll("\\W", "_") + ".dot");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write(printer.printDot());
            } catch (IOException ex) {
                System.err.println("Unable to write scope graph output file to " + outputGraph);
                ex.printStackTrace();
            }
        }
        
        TTimings.endPhase("printing individual scope graphs");
    }
    
    /**                //Load the context from scratch each time
                loadContext("" + run);
     * Resets the diff output location for the given run.
     */
    public void resetDiffOutput(String run) {
        if (outputDiff == null) {
            TOverrides.OUTPUT_DIFF = false;
        } else {
            TDebug.DIFF_OVERRIDE_FILE = outputDiff + "_" + run + ".diff"; //TODO Output a diff per run
            TOverrides.OUTPUT_DIFF = true;
        }
    }
    
    /**
     * Writes the given parse unit to a file (the change to this unit, that is).
     */
    public void writeChangeToFile(ISpoofaxParseUnit unit, String run) {
        if (outputFolder == null) return;
        
        File outputFile = new File(outputFolder, run + "_" + unit.source().getName().getBaseName());
        TAFTermReader taft = new TAFTermReader(S.termFactoryService.getGeneric());
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            taft.unparseToFile(unit.ast(), fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Saves failed results to a file.
     * 
     * @param results
     *      the results
     * 
     * @return
     *      a list of paths for the files that failed
     * 
     * @throws MetaborgException
     *      If an I/O error occurs.
     */
    public List<String> writeFailedResults(ISpoofaxAnalyzeResults results, String run) throws MetaborgException {
        List<String> failed = new ArrayList<>();
        for (ISpoofaxAnalyzeUnit unit : results.results()) {
            for (IMessage msg : unit.messages()) {
                if (msg.severity() == MessageSeverity.ERROR) {
                    failed.add(unit.source().getName().getPath());
                    break;
                }
            }
        }
        
        Collections.sort(failed);
        
        if (outputFolder == null) return failed;
        File file = new File(outputFolder, "failed_" + run + ".txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (String s : failed) {
                bw.write(s);
                bw.newLine();
            }
        } catch (IOException ex) {
            throw new MetaborgException(ex);
        }
        return failed;
    }
    
    public void writeExpectedUsages(List<File> expectedUsages, String run) throws MetaborgException {
        if (outputFolder == null) return;
        File file = new File(outputFolder, "expected_" + run + ".txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (File f : expectedUsages) {
                bw.write(f.getPath());
                bw.newLine();
            }
        } catch (IOException ex) {
            throw new MetaborgException(ex);
        }
    }
    
    /**
     * Writes the different timings csv files.
     */
    public void writeTimings() {
        if (output == null) {
            TTimings.serialize();
            return;
        }
        
        TTimings.serialize(new File(output, "results.csv"));
        List<Long> cleanTimes = new ArrayList<>();
        List<Long> incrementalTimes = new ArrayList<>();
        
        LinkedHashMap<Integer, LinkedHashMap<String, PhaseDetails>> reducedDetail = new LinkedHashMap<>();
        for (Entry<Integer, LinkedHashMap<String, PhaseDetails>> entry : TTimings.results.entrySet()) {
            LinkedHashMap<String, PhaseDetails> map = entry.getValue();
            Boolean type = getPhaseType(map);
            if (type == null) continue;
            
            int phase = entry.getKey();
            LinkedHashMap<String, PhaseDetails> nMap = new LinkedHashMap<>();
            reducedDetail.put(phase, nMap);
            filterDetailed(map).forEachOrdered(e -> nMap.put(e.getKey(), e.getValue()));
            
            Long analysisTime = retrieveAnalysisTime(map);
            if (analysisTime == null) continue;
            if (type) {
                cleanTimes.add(analysisTime);
            } else {
                incrementalTimes.add(analysisTime);
            }
        }
        
        LinkedHashMap<Integer, LinkedHashMap<String, PhaseDetails>> original = TTimings.results;
        TTimings.results = reducedDetail;
        TTimings.serialize(new File(output, "results_reduced.csv"));
        TTimings.results = original;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(output, "analysis_times.csv")))) {
            writer.write("clean first line,incremental second line\n");
            writer.write(cleanTimes.toString().replace("[", "").replace("]", ""));
            writer.write("\n");
            writer.write(incrementalTimes.toString().replace("[", "").replace("]", ""));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Boolean getPhaseType(Map<String, PhaseDetails> map) {
        return map.keySet().stream()
                .filter(s -> s.equals("clean run") || s.equals("incremental run"))
                .map(s -> s.contains("clean"))
                .reduce((a, b) -> {throw new IllegalStateException();})
                .orElse(null);
    }
    
    private Stream<Entry<String, PhaseDetails>> filterDetailed(Map<String, PhaseDetails> map) {
        return map.entrySet().stream().filter(e -> {
            String s = e.getKey();
            if (s.equals("analysis full")) return true;
            if (s.equals("solving")) return true;
            if (s.startsWith("incremental phase")) return true;
            if (s.startsWith("diff ")) return true;
            return false;
        });
    }
    
    private Long retrieveAnalysisTime(Map<String, PhaseDetails> map) {
        return map.entrySet().stream()
                .filter(e -> e.getKey().equals("analysis full"))
                .map(e -> e.getValue())
                .reduce((a, b) -> {throw new IllegalStateException();})
                .map(PhaseDetails::duration)
                .orElse(null);
    }
    
    //---------------------------------------------------------------------------------------------
    //Context
    //---------------------------------------------------------------------------------------------
    
    private void loadContext(String run) throws MetaborgException {
        if (contextIn == null) return;
        TTimings.startPhase("loading context " + run);
        analyze.loadContextFrom(new File(contextIn));
        TTimings.endPhase("loading context " + run);
    }
    
    /**
     * Saves the context.
     * 
     * @throws MetaborgException
     *      If the context cannot be saved.
     */
    private void saveContext() throws MetaborgException {
        if (contextOut == null) return;
        TTimings.startPhase("saving context");
        analyze.saveContextTo(new File(contextOut));
        TTimings.endPhase("saving context");
    }
    
    /**
     * Unloads the context.
     */
    private void unloadContext() {
        if (Context.context() != null) Context.context().wipe();
        analyze.unloadContext();
        System.gc();
        System.gc();
    }
    
    //---------------------------------------------------------------------------------------------
    
    public static void main(String... args) {
        final CommandLine cmd = new CommandLine(new MStatix());
        cmd.parseWithHandlers(new RunLast().andExit(0), CommandLine.defaultExceptionHandler().andExit(1), args);
    }
    
    //---------------------------------------------------------------------------------------------
    //Class loading
    //---------------------------------------------------------------------------------------------
    
    public static void forceInit(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            forceInit(clazz);
        }
    }
    
    /**
     * Force loads a class.
     * 
     * @param klass
     *      the class
     * 
     * @return
     *      the given class
     */
    public static <T> Class<T> forceInit(Class<T> klass) {
        try {
            Class.forName(klass.getName(), true, klass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);  // Can't happen
        }
        return klass;
    } 
}
