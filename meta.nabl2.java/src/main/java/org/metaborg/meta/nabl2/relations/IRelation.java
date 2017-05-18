package org.metaborg.meta.nabl2.relations;

import java.util.Optional;

import io.usethesource.capsule.Set;

public interface IRelation<T> {

    RelationDescription getDescription();

    Set.Immutable<T> smaller(T t);

    Set.Immutable<T> larger(T t);

    boolean contains(T t1, T t2);

    Optional<T> leastUpperBound(T t1, T t2);

    Optional<T> greatestLowerbound(T t1, T t2);

    interface Immutable<T> extends IRelation<T> {

        Transient<T> melt();

    }

    interface Transient<T> extends IRelation<T> {

        boolean add(T t1, T t2) throws RelationException;

        Immutable<T> freeze();

    }

}