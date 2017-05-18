package org.metaborg.meta.nabl2.solver_new;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.UnificationResult;
import org.metaborg.meta.nabl2.unification.VarTracker;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Predicate3;
import org.metaborg.util.time.AggregateTimer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public abstract class ASolver<C extends IConstraint, R> implements ISolver<C, R> {

    private final SolverCore core;
    private final AggregateTimer timer;

    public ASolver(SolverCore core) {
        this.core = core;
        this.timer = new AggregateTimer();
    }

    final public AggregateTimer getTimer() {
        return timer;
    }

    // --- delegate to solver core ---

    protected void addMessage(IMessageInfo message) {
        core.messages.add(message);
    }

    protected ITerm find(ITerm term) {
        return core.unifier.find(term);
    }

    protected void unify(ITerm left, ITerm right, IMessageInfo message) {
        try {
            unify(left, right);
        } catch(UnificationException ex) {
            addMessage(message);
        }
    }

    protected VarTracker<IConstraint> tracker() {
        return core.tracker;
    }

    protected ITermVar fresh(String base) {
        return core.fresh.apply(base);
    }

    protected UnificationResult unify(ITerm left, ITerm right) throws UnificationException {
        return core.unifier.unify(left, right);
    }

    final protected void throwIfCancelled() throws InterruptedException {
        core.cancel.throwIfCancelled();
    }

    final protected void work() {
        core.progress.work(1);
    }

    public boolean iterate() throws InterruptedException {
        return false;
    }

    final protected <CC extends C> boolean doIterate(Collection<CC> constraints, Predicate<CC> solve)
            throws InterruptedException {
        return doIterateAndAdd(constraints,
                c -> solve.test(c) ? Optional.of(Collections.emptySet()) : Optional.empty());
    }

    final protected <CC extends C> boolean doIterateAndAdd(Iterable<CC> constraints,
            Function1<CC, Optional<? extends Iterable<? extends C>>> solve) throws InterruptedException {
        Collection<C> newConstraints = Sets.newHashSet();
        Iterator<CC> it = constraints.iterator();
        boolean progress = false;
        while(it.hasNext()) {
            throwIfCancelled();
            progress |= solve.apply(it.next()).map(cc -> {
                work();
                it.remove();
                Iterables.addAll(newConstraints, cc);
                return true;
            }).orElse(false);
        }
        for(C newConstraint : newConstraints) {
            add(newConstraint);
        }
        return progress;
    }

    final protected <K, L, V> boolean doIterate(IRelation3.Mutable<K, L, V> relation, Predicate3<K, L, V> solve) {
        IRelation3.Mutable<K, L, V> solved = HashRelation3.create();
        for(K key : relation.keySet()) {
            for(Entry<L, V> labelValue : relation.get(key)) {
                if(solve.test(key, labelValue.getKey(), labelValue.getValue())) {
                    solved.put(key, labelValue.getKey(), labelValue.getValue());
                }
            }
        }
        for(K key : solved.keySet()) {
            for(Entry<L, V> labelValue : solved.get(key)) {
                relation.remove(key, labelValue.getKey(), labelValue.getValue());
            }
        }
        return !solved.isEmpty();
    }

}