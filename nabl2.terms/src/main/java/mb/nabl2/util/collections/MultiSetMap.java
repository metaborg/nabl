package mb.nabl2.util.collections;

import io.usethesource.capsule.Map;

public abstract class MultiSetMap<K, V> {

    protected abstract Map<K, MultiSet.Immutable<V>> entries();

    public boolean isEmpty() {
        return entries().isEmpty();
    }

    public boolean containsKey(K key) {
        return entries().containsKey(key);
    }

    public boolean contains(K key, V value) {
        return entries().getOrDefault(key, MultiSet.Immutable.of()).contains(value);
    }

    public int count(K key, V value) {
        return entries().getOrDefault(key, MultiSet.Immutable.of()).count(value);
    }

    public static class Transient<K, V> extends MultiSetMap<K, V> {

        private final Map.Transient<K, MultiSet.Immutable<V>> entries;

        private Transient(Map.Transient<K, MultiSet.Immutable<V>> entries) {
            this.entries = entries;
        }

        @Override public Map.Transient<K, MultiSet.Immutable<V>> entries() {
            return entries;
        }

        public void put(K key, V value) {
            final MultiSet.Transient<V> values = entries.getOrDefault(key, MultiSet.Immutable.of()).melt();
            values.add(value);
            entries.__put(key, values.freeze());
        }

        public MultiSet.Immutable<V> removeKey(K key) {
            if(entries.containsKey(key)) {
                return entries.__remove(key);
            } else {
                return MultiSet.Immutable.of();
            }
        }

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

        public static <K, V> MultiSetMap.Transient<K, V> of() {
            return new Transient<>(Map.Transient.of());
        }

    }

}