package statix.cli.incremental.changes;

import java.io.File;
import java.util.Random;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import statix.cli.StatixAnalyze;
import statix.cli.StatixData;
import statix.cli.StatixParse;
import statix.cli.TestRandomness;

//TODO Make parametric?
/**
 * Interface to represent a (complex) option that can select different files.
 */
public abstract class IIncrementalOptionChange extends IncrementalChange {
    public IIncrementalOptionChange(String group, String sort) {
        super(group, sort);
    }

    /**
     * @return
     *      the options to select from
     * 
     * @throws IllegalStateException
     *      If there are no options to select from.
     */
    public abstract File[] getOptions();
    
    /**
     * @return
     *      the amount of different options
     */
    public int getOptionCount() {
        return getOptions().length;
    }
    
    /**
     * @param random
     *      the random to use
     * 
     * @return
     *      selects an option randomly
     */
    public File selectRandomly(Random random) {
        File[] options = getOptions();
        return options[random.nextInt(options.length)];
    }
    
    /**
     * @param random
     *      the random to use
     * 
     * @return
     *      selects an option randomly
     */
    public File selectRandomly(TestRandomness random) {
        return selectRandomly(random.getRandom());
    }
    
    /**
     * Applies the given option.
     * 
     * @param option
     *      the option
     * 
     * @return
     *      the file representing the given option
     * 
     * @throws ArrayIndexOutOfBoundsException
     *      If the given option is not possible.
     */
    public File apply(int option) {
        return getOptions()[option];
    }
    
    @Override
    public ISpoofaxParseUnit parse(StatixData data, StatixParse parse, StatixAnalyze analyze, TestRandomness random, String file) throws MetaborgException {
        File contents = selectRandomly(random);
        return parse.parse(file, contents);
    }
    
    @Override
    public ISpoofaxParseUnit create(StatixData data, StatixParse parse, StatixAnalyze analyze, TestRandomness random) throws NotApplicableException, MetaborgException {
        String name = data.freshName();
        return parse(data, parse, analyze, random, name);
    }
    
    @Override
    public boolean supportsCreate() {
        return true;
    }
}
