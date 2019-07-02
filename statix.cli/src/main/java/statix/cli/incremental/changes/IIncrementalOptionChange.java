package statix.cli.incremental.changes;

import java.io.File;
import java.util.Random;

//TODO Make parametric?
/**
 * Interface to represent a (complex) option that can select different files.
 */
public interface IIncrementalOptionChange {
    /**
     * @return
     *      the options to select from
     * 
     * @throws IllegalStateException
     *      If there are no options to select from.
     */
    File[] getOptions();
    
    /**
     * @return
     *      the amount of different options
     */
    default int getOptionCount() {
        return getOptions().length;
    }
    
    /**
     * @param random
     *      the random to use
     * 
     * @return
     *      selects an option randomly
     */
    default File selectRandomly(Random random) {
        File[] options = getOptions();
        return options[random.nextInt(options.length)];
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
    default File apply(int option) {
        return getOptions()[option];
    }
}
