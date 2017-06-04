package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;

import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import io.usethesource.capsule.SetMultimap;

public abstract class HashTrieRelation3<K, L, V> implements IRelation3<K, L, V> {

    private final SetMultimap<K, Tuple2<L, V>> fwdK;
    private final SetMultimap<Tuple2<K, L>, V> fwdKL;
    private final SetMultimap<V, Tuple2<L, K>> bwdV;

    protected HashTrieRelation3(SetMultimap<K, Tuple2<L, V>> fwdK, SetMultimap<Tuple2<K, L>, V> fwdKL,
            SetMultimap<V, Tuple2<L, K>> bwdV) {
        this.fwdK = fwdK;
        this.fwdKL = fwdKL;
        this.bwdV = bwdV;
    }

    @Override public boolean contains(K key) {
        return fwdK.containsKey(key);
    }

    @Override public boolean contains(K key, L label) {
        return fwdKL.containsKey(ImmutableTuple2.of(key, label));
    }

    @Override public boolean contains(K key, L label, V value) {
        return fwdK.containsEntry(key, ImmutableTuple2.of(label, value));
    }

    @Override public java.util.Set<K> keySet() {
        return fwdK.keySet();
    }

    @Override public java.util.Set<V> valueSet() {
        return bwdV.keySet();
    }

    @Override public java.util.Set<V> get(K key, L label) {
        return fwdKL.get(ImmutableTuple2.of(key, label));
    }

    @Override public java.util.Set<Tuple2<L, V>> get(K key) {
        return fwdK.get(key);
    }

    public boolean isEmpty() {
        return fwdK.isEmpty();
    }

    @Override public String toString() {
        return fwdK.toString();
    }

    public static class Immutable<K, L, V> extends HashTrieRelation3<K, L, V>
            implements IRelation3.Immutable<K, L, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<K, Tuple2<L, V>> fwdK;
        private final SetMultimap.Immutable<Tuple2<K, L>, V> fwdKL;
        private final SetMultimap.Immutable<V, Tuple2<L, K>> bwdV;
        private final SetMultimap.Immutable<Tuple2<V, L>, K> bwdVL;

        Immutable(SetMultimap.Immutable<K, Tuple2<L, V>> fwdK, SetMultimap.Immutable<Tuple2<K, L>, V> fwdKL,
                SetMultimap.Immutable<V, Tuple2<L, K>> bwdV, SetMultimap.Immutable<Tuple2<V, L>, K> bwdVL) {
            super(fwdK, fwdKL, bwdV);
            this.fwdK = fwdK;
            this.fwdKL = fwdKL;
            this.bwdV = bwdV;
            this.bwdVL = bwdVL;
        }

        @Override public IRelation3.Immutable<V, L, K> inverse() {
            return new HashTrieRelation3.Immutable<>(bwdV, bwdVL, fwdK, fwdKL);
        }

        public HashTrieRelation3.Transient<K, L, V> melt() {
            return new HashTrieRelation3.Transient<>(fwdK.asTransient(), fwdKL.asTransient(), bwdV.asTransient(),
                    bwdVL.asTransient());
        }

        public static <K, L, V> HashTrieRelation3.Immutable<K, L, V> of() {
            return new HashTrieRelation3.Immutable<>(SetMultimap.Immutable.of(), SetMultimap.Immutable.of(),
                    SetMultimap.Immutable.of(), SetMultimap.Immutable.of());
        }

    }

    public static class Transient<K, L, V> extends HashTrieRelation3<K, L, V> implements IRelation3.Transient<K, L, V> {

        private final SetMultimap.Transient<K, Tuple2<L, V>> fwdK;
        private final SetMultimap.Transient<Tuple2<K, L>, V> fwdKL;
        private final SetMultimap.Transient<V, Tuple2<L, K>> bwdV;
        private final SetMultimap.Transient<Tuple2<V, L>, K> bwdVL;

        Transient(SetMultimap.Transient<K, Tuple2<L, V>> fwdK, SetMultimap.Transient<Tuple2<K, L>, V> fwdKL,
                SetMultimap.Transient<V, Tuple2<L, K>> bwdV, SetMultimap.Transient<Tuple2<V, L>, K> bwdVL) {
            super(fwdK, fwdKL, bwdV);
            this.fwdK = fwdK;
            this.fwdKL = fwdKL;
            this.bwdV = bwdV;
            this.bwdVL = bwdVL;
        }

        @Override public boolean put(K key, L label, V value) {
            if(fwdK.__insert(key, ImmutableTuple2.of(label, value))) {
                fwdKL.__insert(ImmutableTuple2.of(key, label), value);
                bwdV.__insert(value, ImmutableTuple2.of(label, key));
                bwdVL.__insert(ImmutableTuple2.of(value, label), key);
                return true;

            }
            return false;
        }

        public boolean putAll(IRelation3<K, L, V> other) {
            return other.stream().reduce(false,
                    (change, klv) -> Boolean.logicalOr(change, put(klv._1(), klv._2(), klv._3())), Boolean::logicalOr);
        }

        @Override public boolean remove(K key) {
            java.util.Set<Tuple2<L, V>> entries;
            if(!(entries = fwdK.get(key)).isEmpty()) {
                fwdK.__remove(key);
                for(Tuple2<L, V> entry : entries) {
                    L label = entry._1();
                    V value = entry._2();
                    fwdKL.__remove(ImmutableTuple2.of(key, label));
                    bwdV.__remove(value);
                    bwdVL.__remove(ImmutableTuple2.of(value, label));
                }
                return true;
            }
            return false;
        }

        @Override public boolean remove(K key, L label) {
            java.util.Set<V> values;
            if(!(values = fwdKL.get(ImmutableTuple2.of(key, label))).isEmpty()) {
                fwdKL.__remove(ImmutableTuple2.of(key, label));
                for(V value : values) {
                    bwdV.__remove(value, ImmutableTuple2.of(label, key));
                    bwdVL.__remove(ImmutableTuple2.of(value, label), key);
                }
                return true;
            }
            return false;
        }

        @Override public boolean remove(K key, L label, V value) {
            if(fwdK.__remove(key, ImmutableTuple2.of(label, value))) {
                fwdKL.__remove(ImmutableTuple2.of(key, label), value);
                bwdV.__remove(value, ImmutableTuple2.of(label, key));
                bwdVL.__remove(ImmutableTuple2.of(value, label), key);
                return true;
            }
            return false;
        }

        @Override public IRelation3.Transient<V, L, K> inverse() {
            return new HashTrieRelation3.Transient<>(bwdV, bwdVL, fwdK, fwdKL);
        }

        public HashTrieRelation3.Immutable<K, L, V> freeze() {
            return new HashTrieRelation3.Immutable<>(fwdK.freeze(), fwdKL.freeze(), bwdV.freeze(), bwdVL.freeze());
        }

        public static <K, L, V> HashTrieRelation3.Transient<K, L, V> of() {
            return new HashTrieRelation3.Transient<>(SetMultimap.Transient.of(), SetMultimap.Transient.of(),
                    SetMultimap.Transient.of(), SetMultimap.Transient.of());
        }

    }

}