package org.metaborg.meta.nabl2.relations.terms;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.metaborg.meta.nabl2.collections.Tuple2;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

import com.google.common.collect.Maps;

public class Relations<T> implements IRelations<T>, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<RelationName,Relation<T>> relations;

    public Relations(Map<RelationName,Relation<T>> relations) {
        this.relations = relations;
    }

    @Override public Iterable<RelationName> getNames() {
        return relations.keySet();
    }

    public void add(IRelationName name, T t1, T t2) throws RelationException {
        if (!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        relations.get(name).add(t1, t2);
    }

    @Override public boolean contains(IRelationName name, T t1, T t2) {
        if (!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        return relations.get(name).contains(t1, t2);
    }

    @Override public Optional<T> leastUpperBound(IRelationName name, T t1, T t2) {
        if (!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        return relations.get(name).leastUpperBound(t1, t2);
    }

    @Override public Optional<T> greatestLowerBound(IRelationName name, T t1, T t2) {
        if (!relations.containsKey(name)) {
            throw new NoSuchElementException("Relation " + name + " not defined.");
        }
        return relations.get(name).greatestLowerbound(t1, t2);
    }

    public static IMatcher<Relations<ITerm>> matcher() {
        return M.listElems(Relation.matcher(), (t, relations) -> {
            Map<RelationName,Relation<ITerm>> r = Maps.newHashMap();
            for (Tuple2<RelationName,Relation<ITerm>> rel : relations) {
                r.put(rel._1(), rel._2());
            }
            return new Relations<>(r);
        });
    }

}