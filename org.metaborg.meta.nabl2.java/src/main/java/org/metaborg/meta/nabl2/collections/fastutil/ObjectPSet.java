package org.metaborg.meta.nabl2.collections.fastutil;

public interface ObjectPSet<T> extends Iterable<T> {

    boolean contains(T elem);

    ObjectPSet<T> add(T elem);

    ObjectPSet<T> remove(T elem);

    boolean isEmpty();

    int size();

}