package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Map.Entry;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public abstract class HashTrieInverseFunction<K, V> implements IInverseFunction<K, V> {

    protected HashTrieInverseFunction() {
    }

    protected abstract SetMultimap<K, V> fwd();

    protected abstract Map<V, K> bwd();

    @Override public boolean containsKey(K key) {
        return fwd().containsKey(key);
    }

    @Override public boolean containsValue(V value) {
        return bwd().containsKey(value);
    }

    @Override public boolean containsEntry(K key, V value) {
        return fwd().containsEntry(key, value);
    }

    @Override public java.util.Set<K> keySet() {
        return fwd().keySet();
    }

    @Override public java.util.Set<Entry<K, V>> entrySet() {
        return fwd().entrySet();
    }

    @Override public java.util.Set<V> valueSet() {
        return bwd().keySet();
    }

    @Override public Set.Immutable<V> get(K key) {
        return fwd().get(key);
    }

    @Override public String toString() {
        return fwd().toString();
    }


    public static class Immutable<K, V> extends HashTrieInverseFunction<K, V>
            implements IInverseFunction.Immutable<K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<K, V> fwd;
        private final Map.Immutable<V, K> bwd;

        Immutable(SetMultimap.Immutable<K, V> fwd, Map.Immutable<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override protected SetMultimap<K, V> fwd() {
            return fwd;
        }

        @Override protected Map<V, K> bwd() {
            return bwd;
        }

        @Override public HashTrieFunction.Immutable<V, K> inverse() {
            return new HashTrieFunction.Immutable<>(bwd, fwd);
        }

        @Override public HashTrieInverseFunction.Transient<K, V> melt() {
            return new HashTrieInverseFunction.Transient<>(fwd.asTransient(), bwd.asTransient());
        }

    }


    public static class Transient<K, V> extends HashTrieInverseFunction<K, V>
            implements IInverseFunction.Transient<K, V> {

        private final SetMultimap.Transient<K, V> fwd;
        private final Map.Transient<V, K> bwd;

        Transient(SetMultimap.Transient<K, V> fwd, Map.Transient<V, K> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override protected SetMultimap<K, V> fwd() {
            return fwd;
        }

        @Override protected Map<V, K> bwd() {
            return bwd;
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

        @Override public HashTrieInverseFunction.Immutable<K, V> freeze() {
            return new HashTrieInverseFunction.Immutable<>(fwd.freeze(), bwd.freeze());
        }

    }


    public static <K, V> IInverseFunction<K, V> union(IInverseFunction<K, V> fun1, IInverseFunction<K, V> fun2) {
        return new Union<>(fun1, fun2);
    }

    private static class Union<K, V> implements IInverseFunction<K, V> {

        private final IInverseFunction<K, V> fun1;
        private final IInverseFunction<K, V> fun2;

        private Union(IInverseFunction<K, V> fun1, IInverseFunction<K, V> fun2) {
            this.fun1 = fun1;
            this.fun2 = fun2;
        }

        @Override public IFunction<V, K> inverse() {
            return HashTrieFunction.union(fun1.inverse(), fun2.inverse());
        }

        @Override public boolean containsKey(K key) {
            return fun1.containsKey(key) || fun2.containsKey(key);
        }

        @Override public boolean containsEntry(K key, V value) {
            return fun1.containsEntry(key, value) || fun2.containsEntry(key, value);
        }

        @Override public boolean containsValue(V value) {
            return fun1.containsValue(value) || fun2.containsValue(value);
        }

        @Override public java.util.Set<K> keySet() {
            return Sets.union(fun1.keySet(), fun2.keySet());
        }

        @Override public java.util.Set<Map.Entry<K, V>> entrySet() {
            return Sets.union(fun1.entrySet(), fun2.entrySet());
        }

        @Override public java.util.Set<V> valueSet() {
            return Sets.union(fun1.valueSet(), fun2.valueSet());
        }

        @Override public java.util.Set<V> get(K key) {
            return Sets.union(fun1.get(key), fun2.get(key));
        }

    }


}