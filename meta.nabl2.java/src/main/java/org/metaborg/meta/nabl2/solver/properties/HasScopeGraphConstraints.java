package org.metaborg.meta.nabl2.solver.properties;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.terms.ITermVar;

public class HasScopeGraphConstraints implements IConstraintSetProperty {

    private int i = 0;

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
            c -> {
                i++;
                return true;
            },
            c -> false,
            c -> false,
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
            c -> {
                i--;
                assert i >= 0 : "Removed more constraints than were added.";
                return true;
            },
            c -> false,
            c -> false,
            c -> false,
            c -> false,
            c -> false,
            c -> false
            // @formatter:on
        ));
    }

    public boolean update(ITermVar var) {
        return false;
    }

    public boolean isEmpty() {
        return i == 0;
    }

}