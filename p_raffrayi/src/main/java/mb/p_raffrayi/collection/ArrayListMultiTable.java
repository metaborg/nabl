package mb.p_raffrayi.collection;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;

/**
 * Multitable implementation that holds duplicate (row/column)-value pairs. Uses
 * {@link HashBasedTable} as backing table, and {@link ArrayList} as backing value collection.
 */
public class ArrayListMultiTable<R, C, V> extends AMultiTable<R, C, V> {
    public static <R, C, V> MultiTable<R, C, V> create() {
        return new ArrayListMultiTable<R, C, V>();
    }


    public ArrayListMultiTable() {
        super(HashBasedTable.<R, C, Collection<V>>create());
    }


    @Override protected ArrayList<V> createCollection() {
        return new ArrayList<>();
    }

    @Override protected ArrayList<V> createCollection(Collection<V> values) {
        return Lists.newArrayList(values);
    }
}
