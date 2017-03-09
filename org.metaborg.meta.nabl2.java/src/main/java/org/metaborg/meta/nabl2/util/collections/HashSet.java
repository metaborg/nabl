package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Iterator;

public class HashSet<E> implements ISet.Mutable<E>, Serializable {

    private static final long serialVersionUID = 42L;

    private final java.util.Set<E> elems;

    private HashSet(java.util.Set<E> elems) {
        this.elems = elems;
    }

    @Override public boolean contains(E elem) {
        return elems.contains(elem);
    }

    @Override public void add(E elem) {
        elems.add(elem);
    }

    @Override public void remove(E elem) {
        elems.remove(elem);
    }

    @Override public Iterator<E> iterator() {
        return elems.iterator();
    }

    public static <E> HashSet<E> create() {
        return new HashSet<>(new java.util.HashSet<>());
    }

}