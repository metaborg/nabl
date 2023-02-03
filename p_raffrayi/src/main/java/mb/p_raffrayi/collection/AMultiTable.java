package mb.p_raffrayi.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public abstract class AMultiTable<R, C, V> implements MultiTable<R, C, V> {
    private final Table<R, C, Collection<V>> table;


    public AMultiTable(Table<R, C, Collection<V>> table) {
        this.table = table;
    }


    protected abstract Collection<V> createCollection();

    protected abstract Collection<V> createCollection(Collection<V> values);


    @Override public boolean contains(R rowKey, C columnKey) {
        return table.contains(rowKey, columnKey);
    }

    @Override public boolean containsRow(R rowKey) {
        return table.containsRow(rowKey);
    }

    @Override public boolean containsColumn(C columnKey) {
        return table.containsColumn(columnKey);
    }

    @Override public Collection<V> get(R rowKey, C columnKey) {
        return table.get(rowKey, columnKey);
    }

    @Override public void clear() {
        table.clear();
    }

    @Override public V put(R rowKey, C columnKey, V value) {
        Collection<V> values = table.get(rowKey, columnKey);
        if(values == null) {
            values = createCollection();
            table.put(rowKey, columnKey, values);
        }
        values.add(value);
        return value;
    }

    @Override public Collection<V> removeAll(R rowKey, C columnKey) {
        final Collection<V> values = table.get(rowKey, columnKey);
        if(values == null) {
            return null;
        }
        final Collection<V> removedValues = createCollection(values);
        values.clear();
        return removedValues;
    }

    @Override public Map<C, Collection<V>> row(R rowKey) {
        return table.row(rowKey);
    }

    @Override public Iterable<V> rowValues(R rowKey) {
        return Iterables2.fromConcat(row(rowKey).values());
    }

    @Override public Map<R, Collection<V>> column(C columnKey) {
        return table.column(columnKey);
    }

    @Override public Iterable<V> columnValues(C columnKey) {
        return Iterables2.fromConcat(column(columnKey).values());
    }

    @Override public Set<Cell<R, C, Collection<V>>> cellSet() {
        return table.cellSet();
    }

    @Override public Set<R> rowKeySet() {
        return table.rowKeySet();
    }

    @Override public Set<C> columnKeySet() {
        return table.columnKeySet();
    }

    @Override public Iterable<V> values() {
        return Iterables2.fromConcat(table.values());
    }

    @Override public Map<R, Map<C, Collection<V>>> rowMap() {
        return table.rowMap();
    }

    @Override public Map<C, Map<R, Collection<V>>> columnMap() {
        return table.columnMap();
    }
}
