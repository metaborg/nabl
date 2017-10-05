package org.metaborg.meta.nabl2.util.collections;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

public class WrappedSet<E> implements ISet.Mutable<E> {

    private final java.util.Set<E> elems;

    private WrappedSet(java.util.Set<E> elems) {
        this.elems = elems;
    }

    @Override public boolean contains(E elem) {
        return elems.contains(elem);
    }

    @Override public boolean isEmpty() {
        return elems.isEmpty();
    }

    @Override public int size() {
        return elems.size();
    }

    @Override public void add(E elem) {
        elems.add(elem);
    }

    @Override public void remove(E elem) {
        elems.remove(elem);
    }

    @Override public Set<E> asSet() {
        return Collections.unmodifiableSet(elems);
    }

    @Override public Stream<E> stream() {
        return elems.stream();
    }

    @Override public Iterator<E> iterator() {
        return elems.iterator();
    }

    public static <E> WrappedSet<E> of(java.util.Set<E> elems) {
        return new WrappedSet<>(elems);
    }

    @Override public String toString() {
        return elems.toString();
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + elems.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") final WrappedSet<E> other = (WrappedSet<E>) obj;
        if(!elems.equals(other.elems))
            return false;
        return true;
    }
}