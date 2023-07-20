package mb.nabl2.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.SetMultimap;
import org.metaborg.util.functions.Function1;

/**
 * Collection of indexed values. Values can be reindexed incrementally, and removed when their indices fully reduce.
 */
public class IndexedBag<V, I> {

    public enum RemovalPolicy {
        ANY, // remove value from the collection when any of its indices are removed
        ALL // remove value from the collection when all of its indices are remonved
    }

    private final MultiSet.Transient<V> values;
    private final SetMultimap<I, Entry> entries;
    private final RemovalPolicy removalPolicy;

    public IndexedBag(RemovalPolicy removalPolicy) {
        this.values = MultiSet.Transient.of();
        this.entries = new SetMultimap<>();
        this.removalPolicy = removalPolicy;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean contains(V value) {
        return values.contains(value);
    }

    /**
     * Return all values in the collection.
     */
    public Collection<V> values() {
        return entries.values().stream().map(e -> e.value).collect(Collectors.toList());
    }

    /**
     * Add a value to the collection, indexed under the given index. Note that the index is not normalized, unless
     * `reindex` is called.
     */
    public boolean add(V value, Iterable<? extends I> indices) {
        values.add(value);
        final Entry entry = new Entry(value);
        for(I index : indices) {
            this.entries.put(index, entry);
            entry.indices.add(index);
        }
        return tryRemove(entry);
    }

    /**
     * Update indices using the normalize function, returning any values for which the index was fully reduced.
     */
    public Collection<V> reindex(I index, Function1<I, ? extends Set<? extends I>> normalize) {
        final Collection<Entry> entries = this.entries.remove(index);
        final Set<? extends I> newIndices = normalize.apply(index);
        if(removalPolicy.equals(RemovalPolicy.ANY) && !newIndices.contains(index)) {
            for(Entry e : entries) {
                e.indices.forEach(i -> this.entries.remove(i, e));
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
        return entries.stream().filter(this::tryRemove).map(v -> v.value).collect(Collectors.toList());
    }

    private boolean tryRemove(Entry entry) {
        if(entry.indices.isEmpty()) {
            values.remove(entry.value);
            return true;
        } else {
            return false;
        }
    }

    private class Entry {

        public final V value;

        public final java.util.Set<I> indices;

        public Entry(V value) {
            this.value = value;
            this.indices = new HashSet<I>();
        }

    }

}