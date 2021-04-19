package mb.nabl2.solver.properties;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.base.IBaseConstraint;
import mb.nabl2.constraints.relations.IRelationConstraint;
import mb.scopegraph.relations.IRelationName;

public class HasRelationBuildConstraints {

    private final Multiset<String> relations;

    public HasRelationBuildConstraints() {
        this.relations = HashMultiset.create();
    }

    public void add(IConstraint constraint) {
        // @formatter:off
        constraint.match(IConstraint.Cases.of(
            c -> null,
            c -> c.match(IBaseConstraint.Cases.of(
                t -> null,
                f -> null,
                cc -> {
                    add(cc.getLeft());
                    add(cc.getRight());
                    return null;
                },
                e -> {
                    add(e.getConstraint());
                    return null;
                },
                n -> null
            )),
            c -> null,
            c -> null,
            c -> null,
            c -> {
                c.match(IRelationConstraint.Cases.of(
                    br -> {
                        br.getRelation().match(IRelationName.Cases.of(
                            name -> {
                                relations.add(name);
                                return null;
                            },
                            extName -> null
                        ));
                        return null;
                    },
                    cr -> null,
                    ev -> null
                ));
                return null;
            },
            c -> null,
            c -> null
        ));
        // @formatter:on
    }

    public void addAll(Iterable<IConstraint> constraints) {
        constraints.forEach(this::add);
    }

    public Set.Immutable<String> remove(IConstraint constraint) {
        // @formatter:off
        return constraint.match(IConstraint.Cases.of(
            c -> Set.Immutable.of(),
            c -> c.match(IBaseConstraint.Cases.of(
                t -> Set.Immutable.of(),
                f -> Set.Immutable.of(),
                cc -> Set.Immutable.union(remove(cc.getLeft()), remove(cc.getRight())),
                e -> remove(e.getConstraint()),
                n -> Set.Immutable.of()
            )),
            c -> Set.Immutable.of(),
            c -> Set.Immutable.of(),
            c -> Set.Immutable.of(),
            c -> c.match(IRelationConstraint.Cases.of(
                br -> br.getRelation().match(IRelationName.Cases.of(
                    name -> {
                        if(relations.remove(name) && relations.count(name) == 0) {
                            return Set.Immutable.of(name);
                        } else {
                            return Set.Immutable.of();
                        }
                    },
                    extName -> Set.Immutable.of()
                )),
                cr -> Set.Immutable.of(),
                ev -> Set.Immutable.of()
            )),
            c -> Set.Immutable.of(),
            c -> Set.Immutable.of()
        ));
        // @formatter:on
    }

    public boolean contains(String name) {
        return relations.contains(name);
    }

}