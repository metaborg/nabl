package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Optional;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.SetMultimap;

public abstract class HashTrieFunction<K, V> implements IFunction<K, V> {

    private final Map<K, V> fwd;
    private final SetMultimap<V, K> bwd;

    protected HashTrieFunction(Map<K, V> fwd, SetMultimap<V, K> bwd) {
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
        return bwd.containsEntry(value, key);
    }

    @Override public java.util.Set<K> keySet() {
        return fwd.keySet();
    }

    @Override public java.util.Set<V> valueSet() {
        return bwd.keySet();
    }

    @Override public Optional<V> get(K key) {
        return Optional.ofNullable(fwd.get(key));
    }

    @Override public String toString() {
        return fwd.toString();
    }

    public static class Immutable<K, V> extends HashTrieFunction<K, V>
            implements IFunction.Immutable<K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Map.Immutable<K, V> fwd;
        private final SetMultimap.Immutable<V, K> bwd;

        Immutable(Map.Immutable<K, V> fwd, SetMultimap.Immutable<V, K> bwd) {
            super(fwd, bwd);
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public HashTrieInverseFunction.Immutable<V, K> inverse() {
            return new HashTrieInverseFunction.Immutable<>(bwd, fwd);
        }

        @Override public HashTrieFunction.Transient<K, V> melt() {
            return new HashTrieFunction.Transient<>(fwd.asTransient(), bwd.asTransient());
        }

        public static <K, V> HashTrieFunction.Immutable<K, V> of() {
            return new HashTrieFunction.Immutable<>(Map.Immutable.of(), SetMultimap.Immutable.of());
        }

    }

    public static class Transient<K, V> extends HashTrieFunction<K, V> implements IFunction.Transient<K, V> {

        private final Map.Transient<K, V> fwd;
        private final SetMultimap.Transient<V, K> bwd;

        Transient(Map.Transient<K, V> fwd, SetMultimap.Transient<V, K> bwd) {
            super(fwd, bwd);
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public boolean put(K key, V value) {
            if(fwd.containsKey(key)) {
                if(value.equals(fwd.get(key))) {
                    return false;
                } else {
                    throw new IllegalArgumentException("Already in domain.");
                }
            }
            fwd.__put(key, value);
            bwd.__insert(value, key);
            return true;
        }

        @Override public boolean putAll(IFunction<K, V> other) {
            return other.stream().reduce(false, (change, kv) -> Boolean.logicalOr(change, put(kv._1(), kv._2())),
                    Boolean::logicalOr);
        }

        @Override public boolean remove(K key) {
            V value = fwd.__remove(key);
            if(value != null) {
                bwd.__remove(value, key);
                return true;
            }
            return false;
        }

        @Override public HashTrieInverseFunction.Transient<V, K> inverse() {
            return new HashTrieInverseFunction.Transient<>(bwd, fwd);
        }

        @Override public HashTrieFunction.Immutable<K, V> freeze() {
            return new HashTrieFunction.Immutable<>(fwd.freeze(), bwd.freeze());
        }

        public static <K, V> HashTrieFunction.Transient<K, V> of() {
            return new HashTrieFunction.Transient<>(Map.Transient.of(), SetMultimap.Transient.of());
        }

    }

}