package mb.scopegraph.oopsla20.diff;

import java.util.Collection;
import java.util.Collections;

import org.metaborg.util.collection.BagMap;
import org.metaborg.util.collection.MultiSetMap;

public abstract class BiMultimap<K, V> {

    public abstract Collection<V> getValues(K key);

    public abstract Collection<K> getKeys(V value);

    public abstract boolean containsValue(V value);

    public static class Transient<K, V> extends BiMultimap<K, V> {

        private MultiSetMap.Transient<K, V> fwd;
        private MultiSetMap.Transient<V, K> bwd;

        private Transient(MultiSetMap.Transient<K, V> fwd, MultiSetMap.Transient<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        public void put(K key, V value) {
            fwd.put(key, value);
            bwd.put(value, key);
        }

        @Override public Collection<V> getValues(K key) {
            return fwd.get(key).toCollection();
        }

        @Override public Collection<K> getKeys(V value) {
            return bwd.get(value).toCollection();
        }

        protected Collection<K> removeKeys(V value) {
            final Collection<K> ks = bwd.removeAll(value);
            fwd.removeAll(ks, value);
            return ks;
        }

        public Collection<V> removeValues(K key) {
            final Collection<V> vs = fwd.removeAll(key);
            bwd.removeAll(vs, key);
            return vs;
        }

        @Override public boolean containsValue(V value) {
            return bwd.containsKey(value);
        }

        public Immutable<K, V> freeze() {
            return new Immutable<>(fwd.freeze(), bwd.freeze());
        }

        public static <K, V> Transient<K, V> of() {
            return new Transient<>(MultiSetMap.Transient.of(), MultiSetMap.Transient.of());
        }
    }

    public static class Immutable<K, V> extends BiMultimap<K, V> {

        private MultiSetMap.Immutable<K, V> fwd;
        private MultiSetMap.Immutable<V, K> bwd;

        private Immutable(MultiSetMap.Immutable<K, V> fwd, MultiSetMap.Immutable<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public Collection<V> getValues(K key) {
            return fwd.get(key).toCollection();
        }

        @Override public Collection<K> getKeys(V value) {
            return bwd.get(value).toCollection();
        }

        @Override public boolean containsValue(V value) {
            return bwd.containsKey(value);
        }

        public Transient<K, V> melt() {
            return new Transient<>(fwd.melt(), bwd.melt());
        }

        public static <K, V> Immutable<K, V> of() {
            return new Immutable<>(MultiSetMap.Immutable.of(), MultiSetMap.Immutable.of());
        }

    }
}
