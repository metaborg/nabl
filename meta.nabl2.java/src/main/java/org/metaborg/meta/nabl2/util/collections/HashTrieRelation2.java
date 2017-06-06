package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Map;

import io.usethesource.capsule.SetMultimap;

public abstract class HashTrieRelation2<K, V> implements IRelation2<K, V> {

    private final SetMultimap<K, V> fwd;
    private final SetMultimap<V, K> bwd;

    protected HashTrieRelation2(SetMultimap<K, V> fwd, SetMultimap<V, K> bwd) {
        this.fwd = fwd;
        this.bwd = bwd;
    }

    @Override public boolean containsKey(K key) {
        return fwd.containsKey(key);
    }

    @Override public boolean containsEntry(K key, V value) {
        return fwd.containsEntry(key, value);
    }

    @Override public boolean containsValue(V value) {
        return bwd.containsKey(value);
    }

    @Override public java.util.Set<K> keySet() {
        return fwd.keySet();
    }

    @Override public java.util.Set<V> valueSet() {
        return bwd.keySet();
    }

    @Override public java.util.Set<Map.Entry<K, V>> entrySet() {
        return fwd.entrySet();
    }

    @Override public java.util.Set<V> get(K key) {
        return fwd.get(key);
    }

    public boolean isEmpty() {
        return fwd.isEmpty();
    }

    @Override public String toString() {
        return fwd.toString();
    }

    public static class Immutable<K, V> extends HashTrieRelation2<K, V>
            implements IRelation2.Immutable<K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<K, V> fwd;
        private final SetMultimap.Immutable<V, K> bwd;

        Immutable(SetMultimap.Immutable<K, V> fwd, SetMultimap.Immutable<V, K> bwd) {
            super(fwd, bwd);
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public IRelation2.Immutable<V, K> inverse() {
            return new HashTrieRelation2.Immutable<>(bwd, fwd);
        }

        public HashTrieRelation2.Transient<K, V> melt() {
            return new HashTrieRelation2.Transient<>(fwd.asTransient(), bwd.asTransient());
        }

        public static <K, V> HashTrieRelation2.Immutable<K, V> of() {
            return new HashTrieRelation2.Immutable<>(SetMultimap.Immutable.of(), SetMultimap.Immutable.of());
        }

    }

    public static class Transient<K, V> extends HashTrieRelation2<K, V> implements IRelation2.Transient<K, V> {

        private final SetMultimap.Transient<K, V> fwd;
        private final SetMultimap.Transient<V, K> bwd;

        Transient(SetMultimap.Transient<K, V> fwd, SetMultimap.Transient<V, K> bwd) {
            super(fwd, bwd);
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public boolean put(K key, V value) {
            if(fwd.__insert(key, value)) {
                bwd.__insert(value, key);
                return true;

            }
            return false;
        }

        public boolean putAll(IRelation2<K, V> other) {
            return other.stream().reduce(false, (change, klv) -> Boolean.logicalOr(change, put(klv._1(), klv._2())),
                    Boolean::logicalOr);
        }

        @Override public boolean removeKey(K key) {
            java.util.Set<V> values;
            if(!(values = fwd.get(key)).isEmpty()) {
                fwd.__remove(key);
                for(V value : values) {
                    bwd.__remove(value, key);
                }
                return true;
            }
            return false;
        }

        @Override public boolean removeValue(V value) {
            java.util.Set<K> keys;
            if(!(keys = bwd.get(value)).isEmpty()) {
                bwd.__remove(value);
                for(K key : keys) {
                    fwd.__remove(key, value);
                }
                return true;
            }
            return false;
        }

        @Override public boolean removeEntry(K key, V value) {
            if(fwd.__remove(key, value)) {
                bwd.__remove(value, key);
                return true;
            }
            return false;
        }

        @Override public IRelation2.Transient<V, K> inverse() {
            return new HashTrieRelation2.Transient<>(bwd, fwd);
        }

        public HashTrieRelation2.Immutable<K, V> freeze() {
            return new HashTrieRelation2.Immutable<>(fwd.freeze(), bwd.freeze());
        }

        public static <K, V> HashTrieRelation2.Transient<K, V> of() {
            return new HashTrieRelation2.Transient<>(SetMultimap.Transient.of(), SetMultimap.Transient.of());
        }

    }

}