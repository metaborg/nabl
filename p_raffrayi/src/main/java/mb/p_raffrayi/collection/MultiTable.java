package mb.p_raffrayi.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Interface for tables that support multiple values. Based on {@link Table} interface.
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

    Set<Cell<R, C, Collection<V>>> cellSet();

    Set<R> rowKeySet();

    Set<C> columnKeySet();

    Iterable<V> values();

    Map<R, Map<C, Collection<V>>> rowMap();

    Map<C, Map<R, Collection<V>>> columnMap();
}
