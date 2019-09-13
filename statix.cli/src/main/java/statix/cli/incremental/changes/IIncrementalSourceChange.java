package statix.cli.incremental.changes;

import java.io.File;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import statix.cli.StatixAnalyze;
import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.StatixUtil;
import statix.cli.TestRandomness;

/**
 * An incremental transformation that applies to source code.
 */
public abstract class IIncrementalSourceChange extends IncrementalChange {
    public IIncrementalSourceChange(String group, String sort) {
        super(group, sort);
    }

    /**
     * Applies this source transformation to the given source.
     * 
     * @param data
     *      the statix data
     * @param original
     *      the original source code
     * 
     * @return
     *      the updated source code
     * 
     * @throws NotApplicableException
     *      If this change is not applicable to the given source code.
     */
    public abstract String apply(StatixData data, String original) throws NotApplicableException;
    
    @Override
    public ISpoofaxParseUnit parse(StatixData data, StatixParse parse, StatixAnalyze analyze, TestRandomness random, String file) throws MetaborgException {
        String original = StatixUtil.readFile(new File(file));
        String newContents = apply(data, original);
        return parse.parse(file, newContents);
    }
}
