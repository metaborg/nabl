package mb.statix.taico.solver.store;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITermVar;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.log.IDebugContext;

public class ModuleConstraintStore implements IConstraintStore {
    private final Queue<IConstraint> active;
    private final Queue<IConstraint> stuckBecauseStuck;
    private final Multimap<ITermVar, IConstraint> stuckOnVar;
    private final Multimap<CriticalEdge, IConstraint> stuckOnEdge;
    
    private boolean progress;
    private AtomicBoolean progressCheck = new AtomicBoolean();
    
    public ModuleConstraintStore(Iterable<? extends IConstraint> constraints, IDebugContext debug) {
        this.active = new LinkedList<>();
        this.stuckBecauseStuck = new LinkedList<>();
        this.stuckOnVar = HashMultimap.create();
        this.stuckOnEdge = HashMultimap.create();
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
        for (IConstraint constraint : stuckBecauseStuck) {
            active.add(constraint);
        }
        stuckBecauseStuck.clear();
        progress = false;
    }
    
    //TODO TAICO Are critical edges determined from the constraints that are left?
    
    //TODO TAICO Orchestrate the solvers in such a way that 
    
    /**
     * The solver is guaranteed to be done if it has no more constraints.
     * It should be able to be done even if there are child solvers still solving.
     * 
     * @return
     *      true if this solver is done, false otherwise
     */
    public boolean isDone() {
        //The solver is guaranteed to be done if it has no more constraints
        //It should be able to become done even if there are child solvers still solving.
        return activeSize() + delayedSize() == 0;
    }
    
    /**
     * Check after a full cycle.
     * 
     * @return
     *      true if there are no constraints is stuck waiting, false otherwise
     */
    public boolean isStuck() {
        return !progress && activeSize() == 0 && delayedSize() != 0;
    }
    
    public void externalProgress() {
        progress = true;
    }
    
    public void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        for (ITermVar var : vars) {
            final Collection<IConstraint> activated;
            activated = stuckOnVar.removeAll(var);
            stuckOnVar.values().removeAll(activated);
            debug.info("activating {}", activated);
            addAll(activated);
        }
    }
    
    public void activateFromEdges(Iterable<? extends CriticalEdge> edges, IDebugContext debug) {
        for (CriticalEdge edge : edges) {
            final Collection<IConstraint> activated;
            activated = stuckOnEdge.removeAll(edge);
            stuckOnEdge.values().removeAll(activated);
            debug.info("activating {}", activated);
            addAll(activated);
        }
    }
    
    public Iterable<IConstraintStore.Entry> active(IDebugContext debug) {
        throw new UnsupportedOperationException("Request elements one by one");
    }
    
    /**
     * Gets an element from the {@link #active} queue. If the queue is empty, this method will take
     * care of activating the stray constraints.
     * 
     * @return
     *      an active constraint, or null if there are no more active constraints
     */
    private IConstraint _getActiveConstraint() {
        IConstraint constraint = active.poll();
        if (constraint != null) return constraint;

        synchronized (this) {
            if (progress) {
                //Do the rollover
                activateStray();
                return active.poll();
            }
        }
        
        //we are stuck (potentially waiting for another solver)
        return constraint;
    }
    
    /**
     * Checks if progress has been made since the last time this method was called.
     * 
     * @return
     *      true if progress was made, false otherwise
     */
    public boolean checkProgressAndReset() {
        return progressCheck.getAndSet(false);
    }
    
    /**
     * Gets an active constraint from this store.
     * 
     * @param debug
     *      the debug context
     * @return
     *      an entry
     */
    public Entry getActiveConstraint(IDebugContext debug) {
        IConstraint constraint = _getActiveConstraint();
        if (constraint == null) return null;
        
        return new Entry() {
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
                }
                //TODO TAICO CRITICALEDGES Implement event based system for propagating this information to other solvers that are interested in it
                //TODO TAICO CRITICALEDGES Implement system for delaying on critical edges
//                else if (!d.criticalEdges().isEmpty()) {
//                    debug.info("delayed {} on critical edges {}", constraint, d.criticalEdges());
//                    for (CriticalEdge edge : d.criticalEdges()) {
//                        stuckOnEdge.put(edge, constraint);
//                    }
//                }
                else {
                    debug.warn("delayed {} for no apparent reason ", constraint);
                    stuckBecauseStuck.add(constraint);
                }
            }

            @Override
            public void remove() {
                synchronized (ModuleConstraintStore.this) {
                    progress = true;
                }
                progressCheck.set(true);
            }
        };
    }
    
    public Map<IConstraint, Delay> delayed() {
        Builder<IConstraint, Delay> delayed = ImmutableMap.builder();
        stuckBecauseStuck.stream().forEach(c -> delayed.put(c, Delay.of()));
        stuckOnVar.entries().stream().forEach(e -> delayed.put(e.getValue(), Delay.ofVar(e.getKey())));
        stuckOnEdge.entries().stream().forEach(e -> delayed.put(e.getValue(), Delay.ofCriticalEdge(e.getKey())));
        return delayed.build();
    }
}

