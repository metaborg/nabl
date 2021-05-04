package mb.scopegraph.oopsla20.diff;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public abstract class BiMultimap<K, V> {

    public abstract Collection<V> getValues(K key);

    public abstract Collection<K> getKeys(V value);

    public abstract boolean containsValue(V value);

    public static class Transient<K, V> extends BiMultimap<K, V> {

        private Multimap<K, V> fwd;
        private Multimap<V, K> bwd;

        private Transient(Multimap<K, V> fwd, Multimap<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        public void put(K key, V value) {
            fwd.put(key, value);
            bwd.put(value, key);
        }

        @Override public Collection<V> getValues(K key) {
            return fwd.get(key);
        }

        @Override public Collection<K> getKeys(V value) {
            return bwd.get(value);
        }

        protected Collection<K> removeKeys(V value) {
            fwd.values().removeAll(Collections.singleton(value));
            return bwd.removeAll(value);
        }

        public Collection<V> removeValues(K key) {
            bwd.values().removeAll(Collections.singleton(key));
            return fwd.removeAll(key);
        }

        @Override public boolean containsValue(V value) {
            return bwd.containsKey(value);
        }

        public Immutable<K, V> freeze() {
            return new Immutable<>(ImmutableMultimap.copyOf(fwd), ImmutableMultimap.copyOf(bwd));
        }

        public static <K, V> Transient<K, V> of() {
            return new Transient<>(ArrayListMultimap.create(), ArrayListMultimap.create());
        }
    }

    public static class Immutable<K, V> extends BiMultimap<K, V> {

        private ImmutableMultimap<K, V> fwd;
        private ImmutableMultimap<V, K> bwd;

        private Immutable(ImmutableMultimap<K, V> fwd, ImmutableMultimap<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public Collection<V> getValues(K key) {
            return fwd.get(key);
        }

        @Override public Collection<K> getKeys(V value) {
            return bwd.get(value);
        }

        @Override public boolean containsValue(V value) {
            return bwd.containsKey(value);
        }

        public Transient<K, V> melt() {
            return new Transient<>(ArrayListMultimap.create(fwd), ArrayListMultimap.create(bwd));
        }

        public static <K, V> Immutable<K, V> of() {
            return new Immutable<>(ImmutableMultimap.of(), ImmutableMultimap.of());
        }

    }
}
