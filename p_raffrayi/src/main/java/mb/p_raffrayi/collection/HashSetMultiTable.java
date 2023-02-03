package mb.p_raffrayi.collection;

import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.HashBasedTable;

/**
 * Multitable implementation that does not hold duplicate (row/column)-value. Uses {@link HashBasedTable} as backing
 * table, and {@link HashSet} as backing value collection.
 */
public class HashSetMultiTable<R, C, V> extends AMultiTable<R, C, V> {
    public static <R, C, V> MultiTable<R, C, V> create() {
        return new HashSetMultiTable<R, C, V>();
    }


    public HashSetMultiTable() {
        super(HashBasedTable.<R, C, Collection<V>>create());
    }


    @Override protected HashSet<V> createCollection() {
        return new HashSet<>();
    }

    @Override protected HashSet<V> createCollection(Collection<V> values) {
        return new HashSet<>(values);
    }
}
