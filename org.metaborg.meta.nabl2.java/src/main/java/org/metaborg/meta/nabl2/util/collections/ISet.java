package org.metaborg.meta.nabl2.util.collections;

import java.util.Set;
import java.util.stream.Stream;

public interface ISet<E> extends Iterable<E> {

    boolean contains(E elem);

    boolean isEmpty();

    int size();

    Set<E> asSet();

    Stream<E> stream();

    interface Mutable<E> extends ISet<E> {

        void add(E elem);

        void remove(E elem);

    }

}