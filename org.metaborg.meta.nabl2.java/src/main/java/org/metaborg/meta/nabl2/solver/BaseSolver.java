package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint.CheckedCases;

public class BaseSolver implements ISolverComponent<IBaseConstraint> {

    private static final long serialVersionUID = 1L;

    @Override public Unit add(IBaseConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(t -> {
            return unit;
        }, f -> {
            throw new UnsatisfiableException(f);
        }));
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        return false;
    }

    @Override public void finish() throws UnsatisfiableException {
    }

}