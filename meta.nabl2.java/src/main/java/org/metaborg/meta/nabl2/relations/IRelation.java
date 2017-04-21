package org.metaborg.meta.nabl2.relations;

import java.util.Collection;

public interface IRelation<T> {

    RelationDescription getDescription();

    Collection<T> smaller(T t);

    Collection<T> larger(T t);

    boolean contains(T t1, T t2);

}