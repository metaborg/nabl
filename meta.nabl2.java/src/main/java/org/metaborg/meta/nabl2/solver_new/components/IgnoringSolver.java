package org.metaborg.meta.nabl2.solver_new.components;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.util.Unit;

public class IgnoringSolver<C extends IConstraint> extends ASolver<C, Unit> {

    public IgnoringSolver(SolverCore core) {
        super(core);
    }

    @Override public boolean add(C constraint) throws InterruptedException {
        return false;
    }

    public Unit finish() {
        return Unit.unit;
    }

}