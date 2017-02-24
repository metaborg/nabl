package org.metaborg.meta.nabl2.solver;

import java.util.Iterator;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.CheckedPredicate1;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.time.AggregateTimer;

public abstract class SolverComponent<C extends IConstraint> {

    private final Solver solver;
    private final AggregateTimer timer;

    public SolverComponent(Solver solver) {
        this.solver = solver;
        this.timer = new AggregateTimer();
    }

    final protected Unifier unifier() {
        return solver.unifier;
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

    protected static <C extends IConstraint> boolean doIterate(Iterable<C> constraints,
        CheckedPredicate1<C, UnsatisfiableException> solve) throws UnsatisfiableException, InterruptedException {
        Iterator<C> it = constraints.iterator();
        boolean progress = false;
        while(it.hasNext()) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            try {
                if(solve.test(it.next())) {
                    progress = true;
                    it.remove();
                }
            } catch(UnsatisfiableException e) {
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    final Iterable<? extends C> finish(IMessageInfo messageInfo) throws InterruptedException {
        return doFinish(messageInfo);
    }

    protected Iterable<? extends C> doFinish(IMessageInfo messageInfo) throws InterruptedException {
        return Iterables2.empty();
    }

}