package mb.statix.concurrent.solver.store;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.ImmutableMap.Builder;

import mb.nabl2.terms.ITermVar;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.log.IDebugContext;

/**
 * Concurrent implementation of the constraint store.
 * 
 * <p>Note that the {@link #active(IDebugContext)} method is not supported, and instead is to be
 * replaced with calls to {@link #getActiveConstraint(IDebugContext)}.</p>
 * 
 * <p>The caller <b>must ensure</b> to call {@link ConcurrentEntry#done()} when processing a single
 * constraint has completed completely.</p>
 */
public class ConcurrentConstraintStore implements IConstraintStore {
    private final LinkedBlockingQueue<IConstraint> active;
    private final LinkedBlockingQueue<IConstraint> stuckBecauseStuck;
    private final Multimap<ITermVar, IConstraint> stuckOnVar;
    private final Multimap<CriticalEdge, IConstraint> stuckOnEdge;
    
    private AtomicBoolean progress = new AtomicBoolean();
    private AtomicInteger pending = new AtomicInteger();
    
    public ConcurrentConstraintStore(Iterable<? extends IConstraint> constraints, IDebugContext debug) {
        this.active = new LinkedBlockingQueue<>();
        this.stuckBecauseStuck = new LinkedBlockingQueue<>();
        this.stuckOnVar = Multimaps.synchronizedMultimap(HashMultimap.create());
        this.stuckOnEdge = Multimaps.synchronizedMultimap(HashMultimap.create());
        addAll(constraints);
    }
    
    public int activeSize() {
        return active.size();
    }
    
    public int delayedSize() {
        return stuckBecauseStuck.size() + stuckOnVar.size() + stuckOnEdge.size();
    }
    
    public void addAll(Iterable<? extends IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            active.add(constraint);
        }
    }
    
    public void activateStray() {
        _activateStray();
    }
    
    private synchronized int _activateStray() {
        progress.set(false);
        return stuckBecauseStuck.drainTo(active);
    }
    
    public void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        for (ITermVar var : vars) {
            final Collection<IConstraint> activated;
            synchronized (stuckOnVar) {
                activated = stuckOnVar.removeAll(var);
                stuckOnVar.values().removeAll(activated);
            }
            
            debug.info("activating {}", activated);
            addAll(activated);
        }
    }
    
    public void activateFromEdges(Collection<? extends CriticalEdge> edges, IDebugContext debug) {
        for (CriticalEdge edge : edges) {
            final Collection<IConstraint> activated;
            synchronized (stuckOnEdge) {
                activated = stuckOnEdge.removeAll(edge);
                stuckOnEdge.values().removeAll(activated);
            }
            debug.info("activating {}", activated);
            addAll(activated);
        }
    }
    
    public Iterable<IConstraintStore.Entry> active(IDebugContext debug) {
        throw new UnsupportedOperationException("Request elements one by one");
    }
    
    /**
     * Gets an element from the {@link #active} queue. If the queue is empty, this method will take
     * care of activating the stray constraints.<p>
     * 
     * When this method returns null, it is <b>guaranteed</b> that there are no more active
     * constraints and that no new active constraints will be added.
     * 
     * @return
     *      an active constraint, or null if there are no more active constraints
     * @throws InterruptedException
     *      If we are interrupted while waiting for a rollover moment.
     */
    private IConstraint _getActiveConstraint() throws InterruptedException {
        IConstraint constraint;
        do {
            constraint = active.poll();
            if (constraint == null) {
                synchronized (this) {
                    //Now that we have the lock, check if the situation has been resolved yet
                    constraint = active.poll();
                    if (constraint != null) break;
                    
                    //Check if there are constraints pending. We only want one thread to perform the rollover.
                    if (pending.get() == 0) {
                        if (progress.getAndSet(false)) {
                            //Progress has been made, roll over
                            if (_activateStray() == 0) return null;
                            
                            //There are new elements, just go back to the outer loop
                            continue;
                        } else {
                            //Nothing is pending, no progress has been made. Terminate
                            return null;
                        }
                    }
                }
                
                //There are still others pending, wait until they are finished or until we can continue
                while (pending.get() != 0 && active.isEmpty()) {
                    this.wait();
                }
            }
        } while (constraint == null);
        return constraint;
    }
    
    /**
     * <b>NOTE: This method can block waiting for new items.</b><br>
     * Gets an active constraint from this store.<p>
     * 
     * When this method returns null, it is <b>guaranteed</b> that there are no more active
     * constraints and that no new active constraints will be added.<p>
     * 
     * Correct usage:
     * <pre>
     * ConcurrentEntry entry = store.getActiveConstraint(debug);
     * if (entry != null) {
     *     try {
     *         //...
     *     } finally {
     *         entry.done();
     *     }
     * }
     * </pre>
     * 
     * @param debug
     *      the debug context
     * @return
     *      an entry
     * @throws InterruptedException 
     *      If this method is interrupted while waiting for elements to become available.
     */
    public ConcurrentEntry getActiveConstraint(IDebugContext debug) throws InterruptedException {
        IConstraint constraint = _getActiveConstraint();
        if (constraint == null) return null;
        
        pending.incrementAndGet();
        
        return new ConcurrentEntry() {
            @Override
            public IConstraint constraint() {
                return constraint;
            }

            @Override
            public void delay(Delay d) {
                if (!d.vars().isEmpty()) {
                    debug.info("delayed {} on vars {}", constraint, d.vars());
                    for (ITermVar var : d.vars()) {
                        stuckOnVar.put(var, constraint);
                    }
                } else if (!d.criticalEdges().isEmpty()) {
                    debug.info("delayed {} on critical edges {}", constraint, d.criticalEdges());
                    for (CriticalEdge edge : d.criticalEdges()) {
                        stuckOnEdge.put(edge, constraint);
                    }
                } else {
                    debug.warn("delayed {} for no apparent reason ", constraint);
                    stuckBecauseStuck.add(constraint);
                }
            }

            @Override
            public void remove() {
                progress.set(true);
            }

            @Override
            public void done() {
                pending.decrementAndGet();
                ConcurrentConstraintStore.this.notifyAll();
            }
        };
    }
    
    /**
     * Handles a single active constraint with the provided consumer.
     * If there are no active constraints left, the given runnable is executed instead.
     * 
     * <p>This method is provided for convenience over {@link #getActiveConstraint(IDebugContext)},
     * since the caller does not have to care about calling the correct methods for handling
     * ConcurrentEntries.
     * 
     * @param consumer
     *      the consumer for handling the entry
     * @param noMoreElements
     *      the function to execute if there are no more elements, can be null
     * @param debug
     *      the debug context
     * @throws InterruptedException
     *      If we are interrupted while waiting for an element to become available.
     * @see #getActiveConstraint(IDebugContext)
     */
    public void handleActiveConstraint(Consumer<Entry> consumer, Runnable noMoreElements, IDebugContext debug) throws InterruptedException {
        ConcurrentEntry entry = getActiveConstraint(debug);
        if (entry == null) {
            if (noMoreElements != null) noMoreElements.run();
            return;
        }
        
        try {
            consumer.accept(entry);
        } finally {
            entry.done();
        }
    }
    
    public Map<IConstraint, Delay> delayed() {
        Builder<IConstraint, Delay> delayed = ImmutableMap.builder();
        stuckBecauseStuck.stream().forEach(c -> delayed.put(c, Delay.of()));
        synchronized (stuckOnVar) {
            stuckOnVar.entries().stream().forEach(e -> delayed.put(e.getValue(), Delay.ofVar(e.getKey())));
        }
        synchronized (stuckOnEdge) {
            stuckOnEdge.entries().stream().forEach(e -> delayed.put(e.getValue(), Delay.ofCriticalEdge(e.getKey())));
        }
        return delayed.build();
    }
    
    public interface ConcurrentEntry extends IConstraintStore.Entry {
        /**
         * Indicates that the current thread is done with this entry.
         * This method <b>MUST</b> be called at the end to preserve correctness.
         * 
         * Correct usage:
         * <pre>
         * ConcurrentEntry entry = store.getActiveConstraint(debug);
         * if (entry != null) {
         *     try {
         *         //...
         *     } finally {
         *         entry.done();
         *     }
         * }
         * </pre>
         */
        public void done();
    }
}
