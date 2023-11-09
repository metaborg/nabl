package mb.p_raffrayi.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.tuple.Tuple3;

public abstract class AMultiTable<R, C, V> implements MultiTable<R, C, V> {
    private final Map<R, Map<C, Collection<V>>> table;


    public AMultiTable(Map<R, Map<C, Collection<V>>> table) {
        this.table = table;
    }


    protected abstract Collection<V> createCollection();

    protected abstract Collection<V> createCollection(Collection<V> values);

    protected abstract <K> Map<K, Collection<V>> createMap();


    @Override public boolean contains(R rowKey, C columnKey) {
        return table.getOrDefault(rowKey, Collections.emptyMap()).containsKey(columnKey);
    }

    @Override public boolean containsRow(R rowKey) {
        return table.containsKey(rowKey);
    }

    @Override public boolean containsColumn(C columnKey) {
        return table.values().stream().anyMatch(m -> m.containsKey(columnKey));
    }

    @Override public Collection<V> get(R rowKey, C columnKey) {
        return table.getOrDefault(rowKey, Collections.emptyMap()).get(columnKey);
    }

    @Override public void clear() {
        table.clear();
    }

    @Override public V put(R rowKey, C columnKey, V value) {
        final Map<C, Collection<V>> column = table.computeIfAbsent(rowKey, rk -> createMap());
        final Collection<V> values = column.computeIfAbsent(columnKey, ck -> createCollection());
        values.add(value);
        return value;
    }

    @Override public Collection<V> removeAll(R rowKey, C columnKey) {
        final @Nullable Map<C, Collection<V>> column = table.get(rowKey);
        if(column == null) {
            return null;
        }
        return column.remove(columnKey);
    }

    @Override public Map<C, Collection<V>> row(R rowKey) {
        return table.get(rowKey);
    }

    @Override public Iterable<V> rowValues(R rowKey) {
        return Iterables2.fromConcat(row(rowKey).values());
    }

    @Override public Map<R, Collection<V>> column(C columnKey) {
        return table.keySet().stream()
            .collect(Collectors.toMap(Function.identity(), k -> get(k, columnKey)));
    }

    @Override public Iterable<V> columnValues(C columnKey) {
        return Iterables2.fromConcat(column(columnKey).values());
    }

    @Override public Set<Tuple3<R, C, Collection<V>>> cellSet() {
        return table.entrySet().stream().flatMap(row -> row.getValue().entrySet().stream()
                .map(col -> Tuple3.of(row.getKey(), col.getKey(), col.getValue())))
            .collect(Collectors.toSet());
    }

    @Override public Set<R> rowKeySet() {
        return table.keySet();
    }

    @Override public Set<C> columnKeySet() {
        return table.values().stream().flatMap(m -> m.keySet().stream()).collect(Collectors.toSet());
    }

    @Override public Iterable<V> values() {
        return table.values().stream().flatMap(m -> m.values().stream().flatMap(Collection::stream)).collect(Collectors.toSet());
    }

    @Override public Map<R, Map<C, Collection<V>>> rowMap() {
        return table;
    }
}
