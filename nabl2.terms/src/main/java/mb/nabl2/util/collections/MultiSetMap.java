package mb.nabl2.util.collections;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import io.usethesource.capsule.Map;

public abstract class MultiSetMap<K, V> {

    // INVARIANT toMap()/entries never contains empty MultiSet values
    //           Thus, if there is an entry for a key, there is at least one value as well.

    protected abstract Map<K, MultiSet.Immutable<V>> toMap();

    public boolean isEmpty() {
        return toMap().isEmpty();
    }

    public int size() {
        return toMap().entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
    }

    public boolean containsKey(K key) {
        return toMap().containsKey(key);
    }

    public boolean contains(K key, V value) {
        return get(key).contains(value);
    }

    public boolean containsValue(V value) {
        return toMap().values().stream().anyMatch(vs -> vs.contains(value));
    }

    public int count(K key, V value) {
        return get(key).count(value);
    }

    public MultiSet.Immutable<V> get(K key) {
        return toMap().getOrDefault(key, MultiSet.Immutable.of());
    }

    public Set<K> keySet() {
        return toMap().keySet();
    }

    public static class Immutable<K, V> extends MultiSetMap<K, V> implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Map.Immutable<K, MultiSet.Immutable<V>> entries;

        private Immutable(Map.Immutable<K, MultiSet.Immutable<V>> entries) {
            this.entries = entries;
        }

        @Override public Map.Immutable<K, MultiSet.Immutable<V>> toMap() {
            return entries;
        }

        public Immutable<K, V> put(K key, V value) {
            final MultiSet.Immutable<V> values = entries.getOrDefault(key, MultiSet.Immutable.of());
            return new Immutable<>(entries.__put(key, values.add(value)));
        }

        public Immutable<K, V> put(K key, V value, int n) {
            if(n < 0) {
                throw new IllegalArgumentException("Negative count");
            }
            final MultiSet.Immutable<V> values = entries.getOrDefault(key, MultiSet.Immutable.of());
            return new Immutable<>(entries.__put(key, values.add(value, n)));
        }

        public Immutable<K, V> removeKey(K key) {
            return new Immutable<>(entries.__remove(key));
        }

        public Immutable<K, V> remove(K key, V value) {
            final MultiSet.Immutable<V> values = entries.getOrDefault(key, MultiSet.Immutable.of());
            return new Immutable<>(entries.__put(key, values.remove(value)));
        }

        public MultiSetMap.Transient<K, V> melt() {
            return new Transient<>(entries.asTransient());
        }

        public static <K, V> MultiSetMap.Immutable<K, V> of() {
            return new Immutable<>(Map.Immutable.of());
        }

    }

    public static class Transient<K, V> extends MultiSetMap<K, V> {

        private final Map.Transient<K, MultiSet.Immutable<V>> entries;

        private Transient(Map.Transient<K, MultiSet.Immutable<V>> entries) {
            this.entries = entries;
        }

        @Override public Map.Transient<K, MultiSet.Immutable<V>> toMap() {
            return entries;
        }

        /**
         * Add an entry to the map, return the new count.
         */
        public int put(K key, V value) {
            final MultiSet.Transient<V> values = entries.getOrDefault(key, MultiSet.Immutable.of()).melt();
            final int n = values.add(value);
            entries.__put(key, values.freeze());
            return n;
        }

        public int put(K key, V value, int n) {
            if(n < 0) {
                throw new IllegalArgumentException("Negative count");
            }
            final MultiSet.Transient<V> values = entries.getOrDefault(key, MultiSet.Immutable.of()).melt();
            final int m = values.add(value, n);
            entries.__put(key, values.freeze());
            return m;
        }

        public void putAll(K key, Iterable<V> values) {
            for(V value : values) {
                put(key, value);
            }
        }

        public MultiSet.Immutable<V> removeKey(K key) {
            if(entries.containsKey(key)) {
                return entries.__remove(key);
            } else {
                return MultiSet.Immutable.of();
            }
        }

        /**
         * Remove an entry from the map, return the new count.
         */
        public int remove(K key, V value) {
            final MultiSet.Transient<V> values = entries.getOrDefault(key, MultiSet.Immutable.of()).melt();
            final int n = values.remove(value);
            if(values.isEmpty()) {
                entries.__remove(key);
            } else {
                entries.__put(key, values.freeze());
            }
            return n;
        }

        public Immutable<K, V> clear() {
            final Immutable<K, V> cleared =
                    new Immutable<>(Map.Immutable.<K, MultiSet.Immutable<V>>of().__putAll(entries));
            entries.keySet().forEach(entries::__remove);
            return cleared;
        }

        public Immutable<K, V> freeze() {
            return new Immutable<>(entries.freeze());
        }

        public static <K, V> MultiSetMap.Transient<K, V> of() {
            return new Transient<>(Map.Transient.of());
        }

    }

    @Override public String toString() {
        return toMap().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));

    }

}