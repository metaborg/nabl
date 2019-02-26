package mb.statix.scopegraph.reference;

import java.util.List;

/**
 * @author taico
 *
 * @param <V>
 *      the type of data
 */
public interface DataWF<V> {

    boolean wf(List<V> d) throws ResolutionException, InterruptedException;

    static <V> DataWF<V> ANY() {
        return new DataWF<V>() {

            @Override public boolean wf(List<V> d) {
                return true;
            }

        };
    }

}