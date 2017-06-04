package org.metaborg.meta.nabl2.solver.properties;

import org.metaborg.meta.nabl2.constraints.IConstraint;

public interface IConstraintSetProperty {

    boolean add(IConstraint constraint);

    default boolean addAll(Iterable<? extends IConstraint> constraints) {
        boolean change = false;
        for(IConstraint constraint : constraints) {
            change |= add(constraint);
        }
        return change;
    }

    boolean remove(IConstraint constraint);

}