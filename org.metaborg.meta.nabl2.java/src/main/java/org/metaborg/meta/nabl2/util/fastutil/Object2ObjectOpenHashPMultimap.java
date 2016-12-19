package org.metaborg.meta.nabl2.util.fastutil;

import java.util.Set;

import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

public final class Object2ObjectOpenHashPMultimap<K, V> implements Object2ObjectPMultimap<K,V> {

    private Inner<K,V> inner;

    public Object2ObjectOpenHashPMultimap() {
        this.inner = new Store<K,V>();
    }

    private Object2ObjectOpenHashPMultimap(Inner<K,V> inner) {
        this.inner = inner;
    }

    @Override public PSet<V> get(K key) {
        return reroot().store.get(key);
    }

    @Override public Object2ObjectPMultimap<K,V> put(K key, V value) {
        return reroot().put(this, key, value);
    }

    @Override public Set<K> keySet() {
        return reroot().store.keySet();
    }

    private Store<K,V> reroot() {
        return inner.reroot(this);
    }

    private interface Inner<K, V> {

        Store<K,V> reroot(Object2ObjectOpenHashPMultimap<K,V> outer);

    }

    private static class Store<K, V> implements Inner<K,V> {

        private final it.unimi.dsi.fastutil.objects.Object2ObjectMap<K,PSet<V>> store;

        public Store() {
            this.store = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<K,PSet<V>>();
            this.store.defaultReturnValue(HashTreePSet.<V> empty());
        }

        public Object2ObjectOpenHashPMultimap<K,V> put(Object2ObjectOpenHashPMultimap<K,V> outer, K key, V value) {
            final PSet<V> values = store.get(key);
            if (values.contains(value)) {
                return outer;
            }
            Object2ObjectOpenHashPMultimap<K,V> res = new Object2ObjectOpenHashPMultimap<K,V>(this);
            outer.inner = new RemoveValue<>(key, value, res);
            store.put(key, values.plus(value));
            return res;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMultimap<K,V> outer) {
            return this;
        };

    }

    private static class AddKey<K, V> implements Inner<K,V> {

        private final K key;
        private final PSet<V> values;
        private final Object2ObjectOpenHashPMultimap<K,V> next;

        public AddKey(K key, PSet<V> values, Object2ObjectOpenHashPMultimap<K,V> next) {
            this.key = key;
            this.values = values;
            this.next = next;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMultimap<K,V> outer) {
            Store<K,V> store = next.reroot();
            assert !store.store.containsKey(key);
            store.store.put(key, values);
            outer.inner = store;
            next.inner = new RemoveKey<>(key, outer);
            return store;
        };

    }

    private static class RemoveKey<K, V> implements Inner<K,V> {

        private final K key;
        private final Object2ObjectOpenHashPMultimap<K,V> next;

        public RemoveKey(K key, Object2ObjectOpenHashPMultimap<K,V> next) {
            this.key = key;
            this.next = next;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMultimap<K,V> outer) {
            Store<K,V> store = next.reroot();
            assert store.store.containsKey(key);
            PSet<V> oldValues = store.store.get(key);
            store.store.remove(key);
            outer.inner = store;
            next.inner = new AddKey<>(key, oldValues, outer);
            return store;
        };

    }

    private static class AddValue<K, V> implements Inner<K,V> {

        private final K key;
        private final V value;
        private final Object2ObjectOpenHashPMultimap<K,V> next;

        public AddValue(K key, V value, Object2ObjectOpenHashPMultimap<K,V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMultimap<K,V> outer) {
            Store<K,V> store = next.reroot();
            if (store.store.containsKey(key)) {
                PSet<V> values = store.store.get(key);
                assert !values.contains(value);
                store.store.put(key, values.plus(value));
                next.inner = new RemoveValue<>(key, value, outer);
            } else {
                store.store.put(key, HashTreePSet.singleton(value));
                next.inner = new RemoveKey<>(key, outer);
            }
            outer.inner = store;
            return store;
        };

    }

    private static class RemoveValue<K, V> implements Inner<K,V> {

        private final K key;
        private final V value;
        private final Object2ObjectOpenHashPMultimap<K,V> next;

        public RemoveValue(K key, V value, Object2ObjectOpenHashPMultimap<K,V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMultimap<K,V> outer) {
            Store<K,V> store = next.reroot();
            assert store.store.containsKey(key);
            PSet<V> values = store.store.get(key);
            assert values.contains(value);
            store.store.put(key, values.minus(value));
            next.inner = new AddValue<>(key, value, outer);
            outer.inner = store;
            return store;
        };

    }

    @Override public String toString() {
        return reroot().store.toString();
    }

}