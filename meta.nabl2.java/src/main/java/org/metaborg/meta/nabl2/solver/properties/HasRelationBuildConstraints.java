package org.metaborg.meta.nabl2.solver.properties;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.relations.IRelationName;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class HasRelationBuildConstraints implements IConstraintSetProperty {

    private final Multiset<IRelationName> relations;

    public HasRelationBuildConstraints() {
        this.relations = HashMultiset.create();
    }

    @Override public boolean add(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(
            // @formatter:off
            c -> false,
            c -> false,
            c -> false,
            c -> false,
            c -> false,
            c -> c.match(IRelationConstraint.Cases.of(
                br -> relations.add(br.getRelation()),
                cr -> false,
                ev -> false
            )),
            c -> false,
            c -> false,
            c -> false
            // @formatter:on
        ));
    }

    @Override public boolean remove(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(
            // @formatter:off
            c -> false,
            c -> false,
            c -> false,
            c -> false,
            c -> false,
            c -> c.match(IRelationConstraint.Cases.of(
                br -> relations.remove(br.getRelation()),
                cr -> false,
                ev -> false
            )),
            c -> false,
            c -> false,
            c -> false
            // @formatter:on
        ));
    }

    public boolean contains(IRelationName name) {
        return relations.contains(name);
    }

}