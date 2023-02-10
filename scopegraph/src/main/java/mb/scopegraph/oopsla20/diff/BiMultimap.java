package mb.scopegraph.oopsla20.diff;

import java.util.Collection;
import java.util.Collections;

import org.metaborg.util.collection.BagMap;

public abstract class BiMultimap<K, V> {

    public abstract Collection<V> getValues(K key);

    public abstract Collection<K> getKeys(V value);

    public abstract boolean containsValue(V value);

    public static class Transient<K, V> extends BiMultimap<K, V> {

        private BagMap.Transient<K, V> fwd;
        private BagMap.Transient<V, K> bwd;

        private Transient(BagMap.Transient<K, V> fwd, BagMap.Transient<V, K> bwd) {
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
            return new Transient<>(BagMap.Transient.of(), BagMap.Transient.of());
        }
    }

    public static class Immutable<K, V> extends BiMultimap<K, V> {

        private BagMap.Immutable<K, V> fwd;
        private BagMap.Immutable<V, K> bwd;

        private Immutable(BagMap.Immutable<K, V> fwd, BagMap.Immutable<V, K> bwd) {
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
            return new Transient<>(fwd.asTransient(), bwd.asTransient());
        }

        public static <K, V> Immutable<K, V> of() {
            return new Immutable<>(BagMap.Immutable.of(), BagMap.Immutable.of());
        }

    }
}
