package statix.cli.incremental;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import statix.cli.Calculator;
import statix.cli.MStatix;
import statix.cli.StatixAnalyze;
import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.TestRandomness;
import statix.cli.incremental.changes.IDesugaredOutput;
import statix.cli.incremental.changes.IncrementalChange;
import statix.cli.incremental.changes.NotApplicableException;

/**
 * Class for generating/applying incremental changes.
 */
public class IncrementalChangeGenerator {
    private static final ILogger logger = LoggerUtils.logger(IncrementalChangeGenerator.class);
    
    private final StatixData data;
    private final StatixParse parse;
    private final boolean desugar;
    private final StatixAnalyze analyze;
    private final TestRandomness randomness;
    private final List<File> allFiles;
    private final List<File> files;
    private final List<File> removedFiles = new ArrayList<>();
    private final List<IncrementalChange> changes;
    
    
    public IncrementalChangeGenerator(StatixData data, StatixParse parse, boolean desugar, StatixAnalyze analyze, TestRandomness randomness, List<File> files, List<IncrementalChange> changes) {
        this.data = data;
        this.parse = parse;
        this.desugar = desugar;
        this.analyze = analyze;
        this.randomness = randomness;
        this.allFiles = files;
        this.files = new ArrayList<>(files);
        this.changes = changes;
    }
    
    public List<File> getFiles() {
        return files;
    }
    
    public File pickFile() {
        return randomness.pick(files);
    }
    
    public IncrementalChange pickChange() {
        return randomness.pick(changes);
    }
    
    public ISpoofaxParseUnit tryApply(MStatix statix, String run) throws MetaborgException {
        IncrementalChange change = pickChange();
        
        if (change.supportsCreate()) {
            logger.info("Applying " + change + " in creation mode");
            ISpoofaxParseUnit punit = change.create(data, parse, analyze, randomness);
            return desugar && !(change instanceof IDesugaredOutput) ? analyze.desugarAst(punit) : punit;
        }
        
        if (change.hasUsageCount()) {
            int count = change.usageCount();
            LinkedHashMap<File, List<File>> map = Calculator.getOptionsForFileUsages(allFiles, count);
            for (File file : removedFiles) map.remove(file);
            
            while (!map.isEmpty()) {
                int number = randomness.getRandom().nextInt(map.size());
                Entry<File, List<File>> entry = get(number, map);
                File file = entry.getKey();
                
                map.remove(file);
                List<File> expectedUsages = entry.getValue();
                
                try {
                    logger.info("Applying " + change + " in parse mode to " + file);
                    logger.info("Usages of " + file + " are " + expectedUsages);
                    
                    ISpoofaxParseUnit unit = change.parse(data, parse, analyze, randomness, file.toString());
                    files.remove(file);
                    statix.writeExpectedUsages(expectedUsages, run);
                    return desugar && !(change instanceof IDesugaredOutput) ? analyze.desugarAst(unit) : unit;
                } catch (NotApplicableException ex) {
                    continue;
                }
            }
        }
        
        if (change.hasFile()) {
            File file = new File(change.getArguments());
            List<File> expectedUsages = Calculator.getUsages(file, allFiles);
            logger.info("Applying " + change + " in parse mode to " + file);
            logger.info("Usages of " + file + " are " + expectedUsages);
            ISpoofaxParseUnit unit = change.parse(data, parse, analyze, randomness, file.toString());
            files.remove(file);
            removedFiles.add(file);
            return desugar && !(change instanceof IDesugaredOutput) ? analyze.desugarAst(unit) : unit;
        }
        
        if (change.supportsParse()) {
            List<File> options = new ArrayList<>(files);
            Map<File, String> fileContents = Calculator.readFiles(allFiles);
            while (!options.isEmpty()) {
                int number = randomness.getRandom().nextInt(options.size());
                File file = options.remove(number);
                List<File> expectedUsages = Calculator.getUsages(file, fileContents);
                try {
                    logger.info("Applying " + change + " in parse mode to " + file);
                    logger.info("Usages of " + file + " are " + expectedUsages);
                    ISpoofaxParseUnit unit = change.parse(data, parse, analyze, randomness, file.toString());
                    files.remove(file);
                    removedFiles.add(file);
                    return desugar && !(change instanceof IDesugaredOutput) ? analyze.desugarAst(unit) : unit;
                } catch (NotApplicableException ex) {
                    continue;
                }
            }
            
            //None of the files were applicable
            logger.error("Change " + change + " was not applicable to any file in the project!");
            if (changes.size() == 1) throw new MetaborgException("Cannot apply any change, no changes are left to apply!");

            changes.remove(change);
            return tryApply(statix, run);
        }
        
        throw new MetaborgException("Change " + change + " (" + change.getClass().getSimpleName() + ") neither supports creation nor modification!");
    }
    
    /**
     * @param index
     *      the index
     * @param map
     *      the map
     * 
     * @return
     *      the index element of the given linked hash map
     * 
     * @throws IndexOutOfBoundsException
     *      If {@code index >= map.size} or {@code index < 0}.
     */
    private static <K, V> Entry<K, V> get(int index, LinkedHashMap<K, V> map) {
        Iterator<Entry<K, V>> it = map.entrySet().iterator();
        Entry<K, V> e = null;
        for (int i = 0; it.hasNext() && i < index; i++) {
            e = it.next();
        }
        if (e == null) throw new IndexOutOfBoundsException();
        return e;
    }
}
