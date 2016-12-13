package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.terms.ITerm;

public interface IRelations {

    public Iterable<RelationName> getNames();

    public IRelation<ITerm> getRelation(RelationName name);

}
