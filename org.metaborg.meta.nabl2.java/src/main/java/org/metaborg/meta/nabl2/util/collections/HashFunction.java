package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class HashFunction<K, V> implements IFunction.Mutable<K, V>, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<K, V> fwd;
    private final SetMultimap<V, K> bwd;

    HashFunction(Map<K, V> fwd, SetMultimap<V, K> bwd) {
        this.fwd = fwd;
        this.bwd = bwd;
    }

    @Override public IInverseFunction.Mutable<V, K> inverse() {
        return new HashInverseFunction<>(bwd, fwd);
    }

    @Override public boolean put(K key, V value) {
        if(fwd.containsKey(key)) {
            if(value.equals(fwd.get(key))) {
                return false;
            } else {
                throw new IllegalArgumentException("Already in domain.");
            }
        }
        fwd.put(key, value);
        bwd.put(value, key);
        return true;
    }

    @Override public boolean remove(K key) {
        V value = fwd.remove(key);
        if(value != null) {
            bwd.remove(value, key);
            return true;
        }
        return false;
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

    @Override public ISet<K> keySet() {
        return WrappedSet.of(fwd.keySet());
    }

    @Override public ISet<V> valueSet() {
        return WrappedSet.of(bwd.keySet());
    }

    @Override public Optional<V> get(K key) {
        return Optional.ofNullable(fwd.get(key));
    }

    public static <K, V> HashFunction<K, V> create() {
        return new HashFunction<>(new HashMap<>(), HashMultimap.create());
    }

    @Override public String toString() {
        return fwd.toString();
    }

}