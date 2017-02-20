package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.util.iterators.Iterables2;

public class BaseSolver extends AbstractSolverComponent<IBaseConstraint> {

    public BaseSolver() {
    }

    @Override public Class<IBaseConstraint> getConstraintClass() {
        return IBaseConstraint.class;
    }
    
    @Override public Unit add(IBaseConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(t -> {
            return unit;
        }, f -> {
            throw new UnsatisfiableException(constraint.getMessageInfo().withDefault(MessageContent.of("False can never be satisfied.")));
        }));
    }

    @Override public boolean iterate() {
        return false;
    }

    @Override public Iterable<IBaseConstraint> finish() {
        return Iterables2.empty();
    }

}