package org.metaborg.meta.nabl2.solver;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.CheckedPredicate1;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.time.AggregateTimer;

public abstract class SolverComponent<C extends IConstraint> {

    private final Solver solver;
    private final AggregateTimer timer;

    public SolverComponent(Solver solver) {
        this.solver = solver;
        this.timer = new AggregateTimer();
    }

    final protected Unifier<IConstraint, IOccurrence> unifier() {
        return solver.unifier;
    }

    final void throwIfCancelled() throws InterruptedException {
        solver.cancel.throwIfCancelled();
    }

    final protected void work() {
        solver.progress.work(1);
    }

    final protected Function1<String, ITermVar> fresh() {
        return solver.fresh;
    }

    final public AggregateTimer getTimer() {
        return timer;
    }

    final protected boolean isTotal() {
        return SolverMode.TOTAL.equals(solver.mode);
    }

    final protected boolean isPartial() {
        return SolverMode.PARTIAL.equals(solver.mode);
    }

    final Unit add(C constraint) throws UnsatisfiableException {
        return doAdd(constraint);
    }

    protected Unit doAdd(C constraint) throws UnsatisfiableException {
        return Unit.unit;
    }

    final boolean iterate() throws UnsatisfiableException, InterruptedException {
        return doIterate();
    }

    protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        return false;
    }

    final protected <CC extends C> boolean doIterate(Iterable<CC> constraints,
            CheckedPredicate1<CC, UnsatisfiableException> solve) throws UnsatisfiableException, InterruptedException {
        Iterator<CC> it = constraints.iterator();
        boolean progress = false;
        while(it.hasNext()) {
            throwIfCancelled();
            try {
                if(solve.test(it.next())) {
                    progress = true;
                    work();
                    it.remove();
                }
            } catch(UnsatisfiableException e) {
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    final Set<? extends C> finish(IMessageInfo messageInfo) throws InterruptedException {
        return doFinish(messageInfo);
    }

    protected Set<? extends C> doFinish(IMessageInfo messageInfo) throws InterruptedException {
        return Collections.emptySet();
    }

}