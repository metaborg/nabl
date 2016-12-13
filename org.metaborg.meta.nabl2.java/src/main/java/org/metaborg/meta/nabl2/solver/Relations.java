package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;
import java.util.Map;

import org.metaborg.meta.nabl2.relations.Relation;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.terms.ITerm;

public class Relations implements IRelations, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<RelationName,Relation<ITerm>> relations;

    public Relations(Map<RelationName,Relation<ITerm>> relations) {
        this.relations = relations;
    }

    @Override public Iterable<RelationName> getNames() {
        return relations.keySet();
    }

    @Override public Relation<ITerm> getRelation(RelationName name) {
        return relations.get(name);
    }

}