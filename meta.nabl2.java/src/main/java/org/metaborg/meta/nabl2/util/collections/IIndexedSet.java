package org.metaborg.meta.nabl2.util.collections;

import java.util.Collection;
import java.util.Set;

public interface IIndexedSet<E, I> {

    boolean containsElement(E elem);

    boolean containsIndex(I index);

    Set<E> get(I index);

    Set<I> indices();

    Collection<E> values();

    interface Mutable<E, I> extends IIndexedSet<E, I> {

        boolean add(E elem);

        boolean remove(E elem);

    }

}