package org.metaborg.meta.nabl2.relations;

import java.util.Optional;

public interface IRelations<T> {

    java.util.Set<IRelationName> getNames();

    boolean contains(IRelationName name, T t1, T t2);

    Optional<T> leastUpperBound(IRelationName name, T t1, T t2);

    Optional<T> greatestLowerBound(IRelationName name, T t1, T t2);

    interface Immutable<T> extends IRelations<T> {

        Transient<T> melt();

    }

    interface Transient<T> extends IRelations<T> {

        boolean add(IRelationName name, T t1, T t2) throws RelationException;

        Immutable<T> freeze();

    }

}