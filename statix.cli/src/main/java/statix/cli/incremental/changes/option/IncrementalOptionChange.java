package statix.cli.incremental.changes.option;

import java.io.File;

import statix.cli.incremental.changes.IIncrementalOptionChange;

/**
 * Implementation of the incremental option.
 */
public class IncrementalOptionChange implements IIncrementalOptionChange {
    private File baseFolder;
    private String baseName;
    
    public IncrementalOptionChange(File baseFolder, String baseName) {
        this.baseFolder = baseFolder;
        this.baseName = baseName;
    }
    
    @Override
    public File[] getOptions() {
        File[] options = baseFolder.listFiles(f -> f.getName().startsWith(baseName));
        if (options.length == 0) throw new IllegalStateException("No options to select from, for " + baseFolder + "/" + baseName);
        return options;
    }
}
