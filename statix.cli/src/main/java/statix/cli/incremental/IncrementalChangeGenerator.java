package statix.cli.incremental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.TestRandomness;
import statix.cli.incremental.changes.IncrementalChange;
import statix.cli.incremental.changes.NotApplicableException;

/**
 * Class for generating/applying incremental changes.
 */
public class IncrementalChangeGenerator {
    private static final ILogger logger = LoggerUtils.logger(IncrementalChangeGenerator.class);
    
    private final StatixData data;
    private final StatixParse parse;
    private final TestRandomness randomness;
    private final List<File> files;
    private final List<IncrementalChange> changes;
    
    public IncrementalChangeGenerator(StatixData data, StatixParse parse, TestRandomness randomness, List<File> files, List<IncrementalChange> changes) {
        this.data = data;
        this.parse = parse;
        this.randomness = randomness;
        this.files = files;
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
    
    public ISpoofaxParseUnit tryApply() throws MetaborgException {
        IncrementalChange change = pickChange();
        
        if (change.supportsCreate()) {
            logger.info("Applying " + change + " in creation mode");
            return change.create(data, parse, randomness);
        }
        
        if (change.supportsParse()) {
            List<File> options = new ArrayList<>(files);
            while (!options.isEmpty()) {
                int number = randomness.getRandom().nextInt(options.size());
                File file = options.remove(number);
                
                try {
                    logger.info("Applying " + change + " in parse mode to " + file);
                    ISpoofaxParseUnit unit = change.parse(data, parse, randomness, file.toString());
                    files.remove(file);
                    return unit;
                } catch (NotApplicableException ex) {
                    continue;
                }
            }
            
            //None of the files were applicable
            logger.error("Change " + change + " was not applicable to any file in the project!");
            if (changes.size() == 1) throw new MetaborgException("Cannot apply any change, no changes are left to apply!");

            changes.remove(change);
            return tryApply();
        }
        
        throw new MetaborgException("Change " + change + " (" + change.getClass().getSimpleName() + ") neither supports creation nor modification!");
    }
}
