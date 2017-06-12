package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public abstract class HashTrieInverseFunction<K, V> implements IInverseFunction<K, V> {

    private final SetMultimap<K, V> fwd;
    private final Map<V, K> bwd;

    protected HashTrieInverseFunction(SetMultimap<K, V> fwd, Map<V, K> bwd) {
        this.fwd = fwd;
        this.bwd = bwd;
    }

    @Override public boolean containsKey(K key) {
        return fwd.containsKey(key);
    }

    @Override public boolean containsValue(V value) {
        return bwd.containsKey(value);
    }

    @Override public boolean containsEntry(K key, V value) {
        return fwd.containsEntry(key, value);
    }

    @Override public java.util.Set<K> keySet() {
        return fwd.keySet();
    }

    @Override public java.util.Set<V> valueSet() {
        return bwd.keySet();
    }

    @Override public Set.Immutable<V> get(K key) {
        return fwd.get(key);
    }

    @Override public String toString() {
        return fwd.toString();
    }

    public static class Immutable<K, V> extends HashTrieInverseFunction<K, V>
            implements IInverseFunction.Immutable<K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<K, V> fwd;
        private final Map.Immutable<V, K> bwd;

        Immutable(SetMultimap.Immutable<K, V> fwd, Map.Immutable<V, K> bwd) {
            super(fwd, bwd);
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public HashTrieFunction.Immutable<V, K> inverse() {
            return new HashTrieFunction.Immutable<>(bwd, fwd);
        }

        public HashTrieInverseFunction.Transient<K, V> melt() {
            return new HashTrieInverseFunction.Transient<>(fwd.asTransient(), bwd.asTransient());
        }

    }

    public static class Transient<K, V> extends HashTrieInverseFunction<K, V>
            implements IInverseFunction.Transient<K, V> {

        private final SetMultimap.Transient<K, V> fwd;
        private final Map.Transient<V, K> bwd;

        Transient(SetMultimap.Transient<K, V> fwd, Map.Transient<V, K> bwd) {
            super(fwd, bwd);
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public boolean put(K key, V value) {
            if(bwd.containsKey(value)) {
                throw new IllegalArgumentException();
            }
            if(fwd.__insert(key, value)) {
                bwd.__put(value, key);
                return true;
            }
            return false;
        }

        @Override public boolean remove(K key, V value) {
            if(fwd.__remove(key, value)) {
                bwd.__remove(value);
                return true;
            }
            return false;
        }

        @Override public HashTrieFunction.Transient<V, K> inverse() {
            return new HashTrieFunction.Transient<>(bwd, fwd);
        }

        public HashTrieInverseFunction.Immutable<K, V> freeze() {
            return new HashTrieInverseFunction.Immutable<>(fwd.freeze(), bwd.freeze());
        }

    }

}