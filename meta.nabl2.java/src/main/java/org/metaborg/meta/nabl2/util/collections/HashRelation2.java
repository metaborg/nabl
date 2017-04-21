package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class HashRelation2<K, V> implements IRelation2.Mutable<K, V>, Serializable {

    private static final long serialVersionUID = 42L;

    private final SetMultimap<K, V> fwd;
    private final SetMultimap<V, K> bwd;

    private HashRelation2(SetMultimap<K, V> fwd, SetMultimap<V, K> bwd) {
        this.fwd = fwd;
        this.bwd = bwd;
    }

    @Override public IRelation2.Mutable<V, K> inverse() {
        return new HashRelation2<>(bwd, fwd);
    }

    @Override public boolean put(K key, V value) {
        if(fwd.put(key, value)) {
            bwd.put(value, key);
            return true;
        }
        return false;
    }

    @Override public boolean remove(K key, V value) {
        if(fwd.remove(key, value)) {
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
        return fwd.containsEntry(key, value);
    }

    @Override public Set<K> keySet() {
        return Collections.unmodifiableSet(fwd.keySet());
    }

    @Override public Set<V> valueSet() {
        return Collections.unmodifiableSet(bwd.keySet());
    }

    @Override public Set<V> get(K key) {
        return Collections.unmodifiableSet(fwd.get(key));
    }

    public static <K, V> HashRelation2<K, V> create() {
        return new HashRelation2<>(HashMultimap.create(), HashMultimap.create());
    }

    @Override public String toString() {
        return fwd.toString();
    }

}