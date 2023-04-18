package mb.nabl2.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.collection.SetMultimap;
import org.metaborg.util.functions.Function1;

/**
 * Collection of indexed values. Values can be reindexed incrementally, and removed when their indices fully reduce.
 */
public class IndexedBagMultimap<K, V, I> {

    public enum RemovalPolicy {
        ANY, // remove value from the collection when any of its indices are removed
        ALL // remove value from the collection when all of its indices are removed
    }

    private final MultiSetMap.Transient<K, V> values;
    private final SetMultimap<I, Entry> entries;
    private final RemovalPolicy removalPolicy;

    public IndexedBagMultimap(RemovalPolicy removalPolicy) {
        this.values = MultiSetMap.Transient.of();
        this.entries = new SetMultimap<>();
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
    public Collection<V> values() {
        return entries.values().stream().map(e -> e.getValue()).collect(Collectors.toList());
    }

    public Collection<Entry> entries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public Collection<I> indices() {
        return entries.keySet();
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
    public Collection<Entry> reindex(I index, Function1<I, ? extends Set<? extends I>> normalize) {
        final Collection<Entry> entries = this.entries.remove(index);
        final Set<? extends I> newIndices = normalize.apply(index);
        if(removalPolicy.equals(RemovalPolicy.ANY) && !newIndices.contains(index)) {
            for(Entry e : entries) {
                for(I i : e.indices) {
                    this.entries.remove(i, e);
                }
                e.indices.clear();
            }
        } else {
            for(Entry e : entries) {
                e.indices.remove(index);
            }
            for(I newIndex : newIndices) {
                for(Entry entry : entries) {
                    this.entries.put(newIndex, entry);
                    entry.indices.add(newIndex);
                }
            }
        }
        return entries.stream().filter(e -> tryRemove(e)).collect(Collectors.toList());
    }

    public Collection<Entry> reindexAll(Function1<I, ? extends Set<? extends I>> normalize) {
        return entries.keySet().stream().flatMap(i -> reindex(i, normalize).stream())
                .collect(ImList.toImmutableList());
    }

    private boolean tryRemove(Entry entry) {
        if(entry.indices.isEmpty()) {
            values.remove(entry.getKey(), entry.getValue());
            return true;
        } else {
            return false;
        }
    }

    public final class Entry implements Map.Entry<K, V> {

        private final K key;
        private final V value;

        public final java.util.Set<I> indices;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.indices = new HashSet<I>();
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

        public java.util.Set<I> getIndices() {
            return indices;
        }


    }

}