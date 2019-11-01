package statix.cli.incremental.changes.other;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import statix.cli.StatixAnalyze;
import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.TestRandomness;
import statix.cli.incremental.changes.IIncrementalNewFileChange;
import statix.cli.incremental.changes.NotApplicableException;

/**
 * Creates a new unused class file with a fresh name.
 */
public class AddClass extends IIncrementalNewFileChange {
    public static final AddClass instance = new AddClass();
    
    private AddClass() {
        super("class", "add");
    }
    
    @Override
    public ISpoofaxParseUnit create(StatixData data, StatixParse parse, StatixAnalyze analyze, TestRandomness random)
            throws NotApplicableException, MetaborgException {
        String clazz = data.freshName();
        String source = "public class " + clazz + " {}";
        return parse.parse(clazz + ".jav", source);
    }

}
