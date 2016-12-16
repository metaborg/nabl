package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint.CheckedCases;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.util.iterators.Iterables2;

public class BaseSolver implements ISolverComponent<IBaseConstraint> {

    private final Unifier unifier;

    public BaseSolver(Unifier unifier) {
        this.unifier = unifier;
    }

    @Override public Unit add(IBaseConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(t -> {
            return unit;
        }, f -> {
            throw constraint.getMessageInfo().makeException("False can never be satisfied.", Iterables2.empty(), unifier);
        }));
    }

    @Override public boolean iterate() {
        return false;
    }

    @Override public Iterable<UnsatisfiableException> finish() {
        return Iterables2.empty();
    }

}