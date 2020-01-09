package mb.nabl2.util.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Collection of indexed values. Values can be reindexed incrementally, and removed when their indices fully reduce.
 */
public class IndexedBagmap<K, V, I> {

    public enum RemovalPolicy {
        ANY, // remove value from the collection when any of its indices are removed
        ALL // remove value from the collection when all of its indices are removed
    }

    private final MultiSetMap.Transient<K, V> values;
    private final Multimap<I, Entry> entries;
    private final RemovalPolicy removalPolicy;

    public IndexedBagmap(RemovalPolicy removalPolicy) {
        this.values = MultiSetMap.Transient.of();
        this.entries = HashMultimap.create();
        this.removalPolicy = removalPolicy;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean containsKey(K key) {
        return values.containsKey(key);
    }

    public boolean contains(K key, V value) {
        return values.contains(key, value);
    }

    /**
     * Return all values in the collection.
     */
    public Iterable<V> values() {
        return entries.values().stream().map(e -> e.value).collect(Collectors.toList());
    }

    /**
     * Add a value to the collection, indexed under the given index. Note that the index is not normalized, unless
     * `reindex` is called.
     */
    public boolean put(K key, V value, Iterable<? extends I> indices) {
        values.put(key, value);
        final Entry entry = new Entry(key, value);
        for(I index : indices) {
            this.entries.put(index, entry);
            entry.indices.add(index);
        }
        return tryRemove(entry);
    }

    /**
     * Update indices using the normalize function, returning any values for which the index was fully reduced.
     */
    public Collection<Map.Entry<K, V>> reindex(I index, Function1<I, ? extends Iterable<? extends I>> normalize) {
        final Collection<Entry> entries = this.entries.removeAll(index).stream().collect(Collectors.toList());
        final Set<I> newIndices = ImmutableSet.copyOf(normalize.apply(index));
        if(removalPolicy.equals(RemovalPolicy.ANY) && !newIndices.contains(index)) {
            entries.forEach(e -> {
                e.indices.forEach(i -> this.entries.remove(i, e));
                e.indices.clear();
            });
        } else {
            entries.forEach(e -> e.indices.remove(index));
            for(I newIndex : newIndices) {
                for(Entry entry : entries) {
                    this.entries.put(newIndex, entry);
                    entry.indices.add(newIndex);
                }
            }
        }
        return entries.stream().filter(e -> tryRemove(e)).collect(Collectors.toList());
    }

    private boolean tryRemove(Entry entry) {
        if(entry.indices.isEmpty()) {
            values.remove(entry.key, entry.value);
            return true;
        } else {
            return false;
        }
    }

    private class Entry implements Map.Entry<K, V> {

        public final K key;
        public final V value;

        public final Set<I> indices;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.indices = Sets.newHashSet();
        }

        @Override public K getKey() {
            return key;
        }

        @Override public V getValue() {
            return value;
        }

        @Override public V setValue(V value) {
            throw new UnsupportedOperationException();
        }


    }

}