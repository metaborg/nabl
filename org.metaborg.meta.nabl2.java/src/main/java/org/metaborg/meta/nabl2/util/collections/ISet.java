package org.metaborg.meta.nabl2.util.collections;

public interface ISet<E> extends Iterable<E> {

    boolean contains(E elem);

    interface Mutable<E> extends ISet<E> {

        void add(E elem);

        void remove(E elem);

    }

}