package mb.p_raffrayi.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.metaborg.util.tuple.Tuple3;

/**
 * Interface for tables that support multiple values. Based on guava's Table interface.
 * Some methods are commented to minimize the interface to current usage while reimplementing it.
 */
public interface MultiTable<R, C, V> {
    boolean contains(@Nullable R rowKey, @Nullable C columnKey);

    boolean containsRow(@Nullable R rowKey);

    boolean containsColumn(@Nullable C columnKey);

    Collection<V> get(@Nullable R rowKey, @Nullable C columnKey);


    void clear();

    V put(R rowKey, C columnKey, V value);

    Collection<V> removeAll(@Nullable R rowKey, @Nullable C columnKey);


    Map<C, Collection<V>> row(R rowKey);
    
    Iterable<V> rowValues(R rowKey);

    Map<R, Collection<V>> column(C columnKey);
    
    Iterable<V> columnValues(C columnKey);

    Set<Tuple3<R, C, Collection<V>>> cellSet();

    Set<R> rowKeySet();

    Set<C> columnKeySet();

    Iterable<V> values();

    Map<R, Map<C, Collection<V>>> rowMap();

//    Map<C, Map<R, Collection<V>>> columnMap();
}
