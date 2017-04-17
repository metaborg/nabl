package org.metaborg.meta.nabl2.relations;

import java.util.Optional;

public interface IRelations<T> {

    Iterable<? extends IRelationName> getNames();

    boolean contains(IRelationName name, T t1, T t2);

    Optional<T> leastUpperBound(IRelationName name, T t1, T t2);

    Optional<T> greatestLowerBound(IRelationName name, T t1, T t2);

}