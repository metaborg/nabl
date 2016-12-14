package org.metaborg.meta.nabl2.relations;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

public class Bounds<T> {

    private final IRelation<T> relation;

    public Bounds(IRelation<T> relation) {
        assert relation.getDescription().equals(
                RelationDescription.PARTIAL_ORDER) : "Bounds calculation requires a partial order.";
        this.relation = relation;
    }

    public Optional<T> leastUpperBound(T t1, T t2) {
        Set<T> bounds = Sets.newHashSet();
        bounds.addAll(relation.larger(t1));
        bounds.retainAll(relation.larger(t2));
        return bounds.stream().filter(l -> bounds.stream().allMatch(g -> relation.contains(l, g))).findFirst();
    }

    public Optional<T> greatestLowerbound(T t1, T t2) {
        Set<T> bounds = Sets.newHashSet();
        bounds.addAll(relation.smaller(t1));
        bounds.retainAll(relation.smaller(t2));
        return bounds.stream().filter(g -> bounds.stream().allMatch(l -> relation.contains(l, g))).findFirst();
    }

}