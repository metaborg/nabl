package org.metaborg.meta.nabl2.solver;

import java.util.Collection;
import java.util.Iterator;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.CheckedPredicate1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.time.AggregateTimer;

public abstract class AbstractSolverComponent<C extends IConstraint> implements ISolverComponent<C> {

    private final AggregateTimer timer;

    public AbstractSolverComponent() {
        this.timer = new AggregateTimer();
    }

    @Override public Unit add(C constraint) throws UnsatisfiableException, InterruptedException {
        return Unit.unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException, InterruptedException {
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

    @Override public Iterable<? extends C> finish() throws InterruptedException {
        return Iterables2.empty();
    }

    @Override public Collection<C> getNormalizedConstraints(IMessageInfo messageInfo) {
        throw new UnsupportedOperationException();
    }

    @Override public AggregateTimer getTimer() {
        return timer;
    }

}