package org.metaborg.meta.nabl2.solver_new.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.util.Unit;

public class BaseSolver extends ASolver<IBaseConstraint, Unit> {

    public BaseSolver(SolverCore core) {
        super(core);
    }

    @Override public boolean add(IBaseConstraint constraint) throws InterruptedException {
        constraint.match(IBaseConstraint.Cases.of(
            // @formatter:off
            t -> unit,
            f -> {
                addMessage(constraint.getMessageInfo().withDefaultContent(MessageContent.of("False can never be satisfied.")));
                return unit;
            }
            // @formatter:on
        ));
        work();
        return false;
    }

    public Unit finish() {
        return Unit.unit;
    }

}