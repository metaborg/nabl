package statix.cli.incremental.changes;

/**
 * An incremental transformation that applies to source code.
 */
public interface IIncrementalSourceChange {
    /**
     * Applies this source transformation to the given source.
     * 
     * @param original
     *      the original source code
     * 
     * @return
     *      the updated source code
     */
    String apply(String original);
}
