package statix.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResults;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.TTimings;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.RunLast;
import picocli.CommandLine.Spec;
import statix.cli.incremental.IncrementalChangeGenerator;
import statix.cli.incremental.changes.IncrementalChange;
import statix.cli.incremental.changes.ast.AddMethod;
import statix.cli.incremental.changes.ast.RemoveClass;
import statix.cli.incremental.changes.ast.RemoveFile;
import statix.cli.incremental.changes.ast.RemoveMethod;
import statix.cli.incremental.changes.ast.RenameClass;
import statix.cli.incremental.changes.ast.RenameMethod;
import statix.cli.incremental.changes.other.AddClass;

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
    @Option(names = { "-o", "--output" }, description = "csv output for results") private String output;
    @Option(names = { "-s", "--seed" }, description = "seed for random changes") private long seed;
    @Option(names = { "-cin", "--contextin" }, description = "file to load context") private String contextIn;
    @Option(names = { "-cout", "--contextout" }, description = "file to save context", defaultValue = "default.context") private String contextOut;
    @Option(names = { "-c", "--count" }, description = "amount of runs", defaultValue = "1") private int count;
    @Option(names = { "-t", "--threads" }, description = "amount of threads to use", defaultValue = "1") private int threads;
    @Option(names = { "--observerself" }, description = "observer mechanism for own module", defaultValue = "true") private boolean observer;
    
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
            files = identifyFiles();
        } catch (IOException e) {
            throw new MetaborgException("Unable to identify files to parse: ", e);
        }
        
        try {
            boolean clean = ichanges.isEmpty();
            
            for (int run = 0; run < count; run++) {
                //Load the context from scratch each time
                loadContext();
                
                startTimedRun(clean);
                if (clean) {
                    cleanAnalysis(files);
                } else {
                    incrementalAnalysis(files);
                }
                endTimedRun(clean);
            }
        } finally {
            exit();
        }
        return null;
    }
    
    public void init() throws MetaborgException {
        data = new StatixData(S, System.out);
        parse = new StatixParse(S, data.getLanguage(language), data.getMessagePrinter());
        analyze = new StatixAnalyze(S, data.createContext(language), data.getMessagePrinter());
        random = new TestRandomness(seed == 0 ? System.currentTimeMillis() : seed);
        ichanges = IncrementalChange.parse(changes == null ? (changes = new ArrayList<>()) : changes);
        
        //Set the concurrency
        TOverrides.CONCURRENT = threads > 1;
        TOverrides.THREADS = threads;
        
        //Set observer mechanism
        TOverrides.USE_OBSERVER_MECHANISM_FOR_SELF = observer;
        
        //Disable debug logging
        TOverrides.OVERRIDE_LOGLEVEL = true;
        TOverrides.LOGLEVEL = "none";
        
        //Disable the modular override for when we want to do normal solving
        TOverrides.MODULES_OVERRIDE = false;
        
        //We do not want to force clean runs. We are using our own contexts
        TOverrides.CLEAN = false;
    }
    
    /**
     * Performs a clean analysis.
     * 
     * @throws MetaborgException
     */
    public void cleanAnalysis(List<File> files) throws MetaborgException {
        logger.info("Starting clean analysis");
        
        TTimings.startPhase("parsing", "Count: " + files.size());
        List<ISpoofaxParseUnit> parsed = parse.parseFiles(files);
        TTimings.endPhase("parsing");
        
        TTimings.startPhase("analysis full");
        ISpoofaxAnalyzeResults results = analyze.cleanAnalysis(parsed);
        TTimings.endPhase("analysis full");
        
        //TODO Do something with results
        
//        for (ISpoofaxAnalyzeUnit result : results.results()) {
//            result.messages()
//        }
        
        saveContext();
    }
    
    /**
     * Performs an incremental analysis.
     * 
     * @throws MetaborgException
     */
    public void incrementalAnalysis(List<File> files) throws MetaborgException {
        logger.info("Starting incremental analysis");
        
        //First we need to select the change to apply
        TTimings.startPhase("identifying files");
        
        TTimings.endPhase("identifying files");
        
        TTimings.startPhase("incremental change + parse");
        IncrementalChangeGenerator generator = new IncrementalChangeGenerator(data, parse, random, files, ichanges);
        ISpoofaxParseUnit modifiedUnit = generator.tryApply();
        TTimings.endPhase("incremental change + parse");
        
        logger.info("Modified file: " + modifiedUnit.source());
        
        TTimings.startPhase("analysis full");
        ISpoofaxAnalyzeResults results = analyze.analyzeAll(Iterables2.singleton(modifiedUnit));
        TTimings.endPhase("analysis full");
        
        //TODO Do something with results
        
        saveContext();
    }
    
    public void exit() {
        if (output != null) {
            TTimings.serialize(new File(output));
        } else {
            TTimings.serialize();
        }
    }
    
    //---------------------------------------------------------------------------------------------
    
    /**
     * Identifies all the files that should be parsed.
     * 
     * @return
     *      the files
     * 
     * @throws IOException
     *      If reading the contents of the directories encounters an error.
     */
    public List<File> identifyFiles() throws IOException {
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
                "Clean: " + clean,
                "Changes: " + changes,
                "Seed: " + random.getSeed(),
                "Concurrent: " + (threads == 1 ? "false" : "" + threads));
    }
    
    public void endTimedRun(boolean clean) {
        String phase = clean ? "clean run" : "incremental run";
        TTimings.endPhase(phase);
        TTimings.unfixRun();
    }
    
    //---------------------------------------------------------------------------------------------
    
    private void loadContext() throws MetaborgException {
        if (contextIn != null) analyze.loadContextFrom(new File(contextIn));
    }
    
    /**
     * Saves the context.
     * 
     * @throws MetaborgException
     *      If the context cannot be saved.
     */
    private void saveContext() throws MetaborgException {
        if (contextOut != null) analyze.saveContextTo(new File(contextOut));
    }
    
    //---------------------------------------------------------------------------------------------
    
    public static void main(String... args) {
        final CommandLine cmd = new CommandLine(new MStatix());
        cmd.parseWithHandlers(new RunLast().andExit(0), CommandLine.defaultExceptionHandler().andExit(1), args);
    }
    
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
