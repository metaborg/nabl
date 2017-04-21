package org.metaborg.meta.nabl2.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.VarTracker;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.CheckedFunction1;
import org.metaborg.meta.nabl2.util.functions.CheckedPredicate1;
import org.metaborg.util.time.AggregateTimer;

import com.google.common.collect.Sets;

public abstract class SolverComponent<C extends IConstraint> {

    private final Solver solver;
    private final AggregateTimer timer;

    public SolverComponent(Solver solver) {
        this.solver = solver;
        this.timer = new AggregateTimer();
    }

    final void throwIfCancelled() throws InterruptedException {
        solver.cancel.throwIfCancelled();
    }

    final protected void work() {
        solver.progress.work(1);
    }

    final protected ITermVar fresh(String base) {
        return solver.fresh(base);
    }

    final protected VarTracker<IConstraint> tracker() {
        return solver.tracker;
    }

    final protected ITerm find(ITerm term) {
        return solver.find(term);
    }

    final protected void unify(ITerm left, ITerm right, IMessageInfo message) throws UnsatisfiableException {
        solver.unify(left, right, message);
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

    final protected <CC extends C> boolean doIterate(Collection<CC> constraints,
            CheckedPredicate1<CC, UnsatisfiableException> solve) throws UnsatisfiableException, InterruptedException {
        return doIterateAndAdd(constraints, c -> solve.test(c) ? Optional.of(Collections.emptySet()) : Optional.empty());
    }

    final protected <CC extends C> boolean doIterateAndAdd(Collection<CC> constraints,
            CheckedFunction1<CC, Optional<? extends Collection<C>>, UnsatisfiableException> solve)
            throws UnsatisfiableException, InterruptedException {
        Collection<C> newConstraints = Sets.newHashSet();
        Iterator<CC> it = constraints.iterator();
        boolean progress = false;
        while(it.hasNext()) {
            throwIfCancelled();
            try {
                progress |= solve.apply(it.next()).map(cc -> {
                    work();
                    it.remove();
                    newConstraints.addAll(cc);
                    return true;
                }).orElse(false);
            } catch(UnsatisfiableException e) {
                it.remove();
                throw e;
            }
        }
        for(C newConstraint : newConstraints) {
            add(newConstraint);
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