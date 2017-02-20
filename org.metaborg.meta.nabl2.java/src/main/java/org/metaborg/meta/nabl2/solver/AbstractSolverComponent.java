package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.util.time.AggregateTimer;

public abstract class AbstractSolverComponent<C extends IConstraint> implements ISolverComponent<C> {

    private final AggregateTimer timer;

    public AbstractSolverComponent() {
        this.timer = new AggregateTimer();
    }

    @Override public AggregateTimer getTimer() {
        return timer;
    }

}