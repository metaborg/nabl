package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.util.Unit;

public class BaseComponent extends ASolver<IBaseConstraint, Unit> {

    public BaseComponent(SolverCore core) {
        super(core);
    }

    @Override public Optional<SolveResult> solve(IBaseConstraint constraint) throws InterruptedException {
        final SolveResult result = constraint.match(IBaseConstraint.Cases.of(
            // @formatter:off
            t -> SolveResult.empty(),
            f -> SolveResult.messages(
                    constraint.getMessageInfo().withDefaultContent(MessageContent.of("False can never be satisfied.")))
            // @formatter:on
        ));
        return Optional.of(result);
    }

    public Unit finish() {
        return Unit.unit;
    }

}