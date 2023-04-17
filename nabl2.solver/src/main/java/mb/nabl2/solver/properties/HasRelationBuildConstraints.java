package mb.nabl2.solver.properties;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.base.IBaseConstraint;
import mb.nabl2.constraints.relations.IRelationConstraint;
import mb.scopegraph.relations.IRelationName;

public class HasRelationBuildConstraints {

    private final MultiSet.Transient<String> relations;

    public HasRelationBuildConstraints() {
        this.relations = MultiSet.Transient.of();
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
            c -> CapsuleUtil.immutableSet(),
            c -> c.match(IBaseConstraint.Cases.of(
                t -> CapsuleUtil.immutableSet(),
                f -> CapsuleUtil.immutableSet(),
                cc -> Set.Immutable.union(remove(cc.getLeft()), remove(cc.getRight())),
                e -> remove(e.getConstraint()),
                n -> CapsuleUtil.immutableSet()
            )),
            c -> CapsuleUtil.immutableSet(),
            c -> CapsuleUtil.immutableSet(),
            c -> CapsuleUtil.immutableSet(),
            c -> c.match(IRelationConstraint.Cases.of(
                br -> br.getRelation().match(IRelationName.Cases.of(
                    name -> {
                        if(relations.remove(name) > 0 && relations.count(name) == 0) {
                            return CapsuleUtil.immutableSet(name);
                        } else {
                            return CapsuleUtil.immutableSet();
                        }
                    },
                    extName -> CapsuleUtil.immutableSet()
                )),
                cr -> CapsuleUtil.immutableSet(),
                ev -> CapsuleUtil.immutableSet()
            )),
            c -> CapsuleUtil.immutableSet(),
            c -> CapsuleUtil.immutableSet()
        ));
        // @formatter:on
    }

    public boolean contains(String name) {
        return relations.contains(name);
    }

}