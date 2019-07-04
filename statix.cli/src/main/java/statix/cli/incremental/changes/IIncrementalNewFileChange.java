package statix.cli.incremental.changes;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.TestRandomness;

/**
 * Class to represent an incremental change consisting of an addition.
 */
public abstract class IIncrementalNewFileChange extends IncrementalChange {

    public IIncrementalNewFileChange(String group, String sort) {
        super(group, sort);
    }
    
    @Override
    public abstract ISpoofaxParseUnit create(StatixData data, StatixParse parse, TestRandomness random)
            throws NotApplicableException, MetaborgException;
    
    @Override
    public boolean supportsParse() {
        return false;
    }
    
    @Override
    public boolean supportsCreate() {
        return true;
    }
}
