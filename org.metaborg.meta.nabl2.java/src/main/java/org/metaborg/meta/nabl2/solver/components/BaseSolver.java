package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.util.Unit;

public class BaseSolver extends SolverComponent<IBaseConstraint> {

    public BaseSolver(Solver solver) {
        super(solver);
    }

    @Override protected Unit doAdd(IBaseConstraint constraint) throws UnsatisfiableException {
        constraint.matchOrThrow(CheckedCases.of(t -> unit, f -> {
            throw new UnsatisfiableException(
                constraint.getMessageInfo().withDefaultContent(MessageContent.of("False can never be satisfied.")));
        }));
        work();
        return unit;
    }

}