package org.metaborg.meta.nabl2.util.fastutil;

import java.util.Iterator;

public final class ObjectOpenHashPSet<T> implements ObjectPSet<T> {

    private Inner<T> inner;

    public ObjectOpenHashPSet() {
        this.inner = new Store<T>();
    }

    private ObjectOpenHashPSet(Inner<T> inner) {
        this.inner = inner;
    }

    @Override public boolean contains(T elem) {
        return reroot().store.contains(elem);
    }

    @Override public ObjectPSet<T> add(T elem) {
        return reroot().add(this, elem);
    }

    @Override public ObjectPSet<T> remove(T elem) {
        return reroot().remove(this, elem);
    }

    @Override public int size() {
        return reroot().store.size();
    }

    @Override public boolean isEmpty() {
        return reroot().store.isEmpty();
    }

    @Override public Iterator<T> iterator() {
        return reroot().store.iterator();
    }

    private Store<T> reroot() {
        return inner.reroot(this);
    }

    private interface Inner<T> {

        Store<T> reroot(ObjectOpenHashPSet<T> outer);
    }

    private static class Store<T> implements Inner<T> {

        private final it.unimi.dsi.fastutil.objects.ObjectSet<T> store = new it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<T>();

        public ObjectPSet<T> add(ObjectOpenHashPSet<T> outer, T elem) {
            ObjectOpenHashPSet<T> res = new ObjectOpenHashPSet<T>(this);
            if (store.contains(elem)) {
                return outer;
            } else {
                outer.inner = new Remove<>(elem, res);
            }
            store.add(elem);
            return res;
        }

        public ObjectPSet<T> remove(ObjectOpenHashPSet<T> outer, T elem) {
            if (!store.contains(elem)) {
                return outer;
            }
            ObjectOpenHashPSet<T> res = new ObjectOpenHashPSet<T>(this);
            outer.inner = new Add<>(elem, res);
            store.remove(elem);
            return res;
        }

        @Override public Store<T> reroot(ObjectOpenHashPSet<T> outer) {
            return this;
        };
    }

    private static class Add<T> implements Inner<T> {

        private final T elem;
        private final ObjectOpenHashPSet<T> next;

        public Add(T elem, ObjectOpenHashPSet<T> next) {
            this.elem = elem;
            this.next = next;
        }

        @Override public Store<T> reroot(ObjectOpenHashPSet<T> outer) {
            Store<T> store = next.reroot();
            assert !store.store.contains(elem);
            store.store.add(elem);
            outer.inner = store;
            next.inner = new Remove<>(elem, outer);
            return store;
        };

    }

    private static class Remove<T> implements Inner<T> {

        private final T elem;
        private final ObjectOpenHashPSet<T> next;

        public Remove(T elem, ObjectOpenHashPSet<T> next) {
            this.elem = elem;
            this.next = next;
        }

        @Override public Store<T> reroot(ObjectOpenHashPSet<T> outer) {
            Store<T> store = next.reroot();
            assert store.store.contains(elem);
            store.store.remove(elem);
            outer.inner = store;
            next.inner = new Add<>(elem, outer);
            return store;
        };

    }

    @Override public String toString() {
        return reroot().store.toString();
    }

}