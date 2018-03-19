package mb.nabl2.solver.properties;

import java.util.Collection;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.base.IBaseConstraint;
import mb.nabl2.constraints.relations.IRelationConstraint;
import mb.nabl2.relations.IRelationName;
import mb.nabl2.terms.ITermVar;

public class HasRelationBuildConstraints implements IConstraintSetProperty {

    private final Multiset<String> relations;

    public HasRelationBuildConstraints() {
        this.relations = HashMultiset.create();
    }

    @Override public boolean add(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.of(
        // @formatter:off
            c -> false,
            c -> c.match(IBaseConstraint.Cases.of(
                t -> false,
                f -> false,
                cc -> {
                    boolean change = false;
                    change |= add(cc.getLeft());
                    change |= add(cc.getRight());
                    return change;
                },
                e -> add(e.getConstraint()),
                n -> false
            )),
            c -> false,
            c -> false,
            c -> false,
            c -> c.match(IRelationConstraint.Cases.of(
                br -> br.getRelation().match(IRelationName.Cases.of(name -> relations.add(name), extName -> false)),
                cr -> false,
                ev -> false
            )),
            c -> false,
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
            c -> c.match(IBaseConstraint.Cases.of(
                t -> false,
                f -> false,
                cc -> {
                    boolean change = false;
                    change |= remove(cc.getLeft());
                    change |= remove(cc.getRight());
                    return change;
                },
                e -> remove(e.getConstraint()),
                n -> false
            )),
            c -> false,
            c -> false,
            c -> false,
            c -> c.match(IRelationConstraint.Cases.of(
                br -> br.getRelation().match(IRelationName.Cases.of(name -> relations.remove(name), extName -> false)),
                cr -> false,
                ev -> false
            )),
            c -> false,
            c -> false,
            c -> false,
            c -> false
            // @formatter:on
        ));
    }

    public boolean update(Collection<ITermVar> vars) {
        return false;
    }

    public boolean contains(String name) {
        return relations.contains(name);
    }

}