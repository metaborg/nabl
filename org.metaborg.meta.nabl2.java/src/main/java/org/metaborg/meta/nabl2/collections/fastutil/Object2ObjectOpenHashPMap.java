package org.metaborg.meta.nabl2.collections.fastutil;

import java.util.Set;

public final class Object2ObjectOpenHashPMap<K, V> implements Object2ObjectPMap<K,V> {

    private Inner<K,V> inner;

    public Object2ObjectOpenHashPMap() {
        this.inner = new Store<K,V>();
    }

    public Object2ObjectOpenHashPMap(V defaultValue) {
        this.inner = new Store<K,V>(defaultValue);
    }

    private Object2ObjectOpenHashPMap(Inner<K,V> inner) {
        this.inner = inner;
    }

    @Override public boolean containsKey(K key) {
        return reroot().store.containsKey(key);
    }

    @Override public V get(K key) {
        return reroot().store.get(key);
    }

    @Override public Object2ObjectOpenHashPMap<K,V> put(K key, V value) {
        return reroot().put(this, key, value);
    }

    @Override public Object2ObjectOpenHashPMap<K,V> remove(K key) {
        return reroot().remove(this, key);
    }

    @Override public Set<K> keySet() {
        return reroot().store.keySet();
    }

    private Store<K,V> reroot() {
        return inner.reroot(this);
    }

    private interface Inner<K, V> {

        Store<K,V> reroot(Object2ObjectOpenHashPMap<K,V> outer);

    }

    private static class Store<K, V> implements Inner<K,V> {

        private final it.unimi.dsi.fastutil.objects.Object2ObjectMap<K,V> store;

        public Store(V defaultValue) {
            this();
            store.defaultReturnValue(defaultValue);
        }

        public Store() {
            this.store = new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<K,V>();
        }

        public Object2ObjectOpenHashPMap<K,V> put(Object2ObjectOpenHashPMap<K,V> outer, K key, V value) {
            Object2ObjectOpenHashPMap<K,V> res = new Object2ObjectOpenHashPMap<K,V>(this);
            if (store.containsKey(key)) {
                V oldValue = store.get(key);
                outer.inner = new Update<>(key, oldValue, res);
            } else {
                outer.inner = new Remove<>(key, res);
            }
            store.put(key, value);
            return res;
        }

        public Object2ObjectOpenHashPMap<K,V> remove(Object2ObjectOpenHashPMap<K,V> outer, K key) {
            if (!store.containsKey(key)) {
                return outer;
            }
            Object2ObjectOpenHashPMap<K,V> res = new Object2ObjectOpenHashPMap<K,V>(this);
            V oldValue = store.get(key);
            outer.inner = new Add<>(key, oldValue, res);
            store.remove(key);
            return res;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMap<K,V> outer) {
            return this;
        };
    }

    private static class Add<K, V> implements Inner<K,V> {

        private final K key;
        private final V value;
        private final Object2ObjectOpenHashPMap<K,V> next;

        public Add(K key, V value, Object2ObjectOpenHashPMap<K,V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMap<K,V> outer) {
            Store<K,V> store = next.reroot();
            assert !store.store.containsKey(key);
            store.store.put(key, value);
            outer.inner = store;
            next.inner = new Remove<>(key, outer);
            return store;
        };

    }

    private static class Update<K, V> implements Inner<K,V> {

        private final K key;
        private final V value;
        private final Object2ObjectOpenHashPMap<K,V> next;

        public Update(K key, V value, Object2ObjectOpenHashPMap<K,V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMap<K,V> outer) {
            Store<K,V> store = next.reroot();
            assert store.store.containsKey(key);
            V oldValue = store.store.get(key);
            store.store.put(key, value);
            outer.inner = store;
            next.inner = new Update<>(key, oldValue, outer);
            return store;
        };

    }

    private static class Remove<K, V> implements Inner<K,V> {

        private final K key;
        private final Object2ObjectOpenHashPMap<K,V> next;

        public Remove(K key, Object2ObjectOpenHashPMap<K,V> next) {
            this.key = key;
            this.next = next;
        }

        @Override public Store<K,V> reroot(Object2ObjectOpenHashPMap<K,V> outer) {
            Store<K,V> store = next.reroot();
            assert store.store.containsKey(key);
            V oldValue = store.store.get(key);
            store.store.remove(key);
            outer.inner = store;
            next.inner = new Add<>(key, oldValue, outer);
            return store;
        };

    }

    @Override public String toString() {
        return reroot().store.toString();
    }

}