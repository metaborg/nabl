package mb.p_raffrayi.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

/**
 * Multitable implementation that does not hold duplicate (row/column)-value. Uses a nested {@link HashMap} as backing
 * table, and {@link HashSet} as backing value collection.
 */
public class HashSetMultiTable<R, C, V> extends AMultiTable<R, C, V> {
    public static <R, C, V> MultiTable<R, C, V> create() {
        return new HashSetMultiTable<R, C, V>();
    }


    public HashSetMultiTable() {
        super(new HashMap<R, Map<C, Collection<V>>>());
    }


    @Override protected HashSet<V> createCollection() {
        return new HashSet<>();
    }

    @Override protected HashSet<V> createCollection(Collection<V> values) {
        return new HashSet<>(values);
    }

    @Override protected Map<C, Collection<V>> createMap() {
        return new HashMap<>();
    }
}
