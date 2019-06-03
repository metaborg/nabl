package mb.statix.taico.solver.store;

import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.taico.util.TDebug.STORE_DEBUG;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.TDebug;
import mb.statix.taico.util.Vars;

public class ModuleConstraintStore implements IConstraintStore {
    private final String owner;
    private final Queue<IConstraint> active;
    private final Multimap<ITermVar, Delayed> stuckOnVar;
    private final Multimap<CriticalEdge, Delayed> stuckOnEdge;
    private final Multimap<String, Delayed> stuckOnModule;
    
    private final SetMultimap<CriticalEdge, ModuleConstraintStore> edgeObservers;
    private final Multimap<ITermVar, ModuleConstraintStore> varObservers;
    
    private volatile IObserver<ModuleConstraintStore> observer;
    private volatile boolean progress;
    
    private final Object variableLock = new Object();
    
    public ModuleConstraintStore(String owner, Iterable<? extends IConstraint> constraints, IDebugContext debug) {
        this.owner = owner;
        this.active = new LinkedBlockingQueue<>();
        this.stuckOnVar = HashMultimap.create();
        this.stuckOnEdge = HashMultimap.create();
        this.stuckOnModule = HashMultimap.create();
        this.edgeObservers = MultimapBuilder.hashKeys().hashSetValues().build();
        this.varObservers = HashMultimap.create();
        addAll(constraints);
    }
    
    public IObserver<ModuleConstraintStore> getStoreObserver() {
        return this.observer;
    }
    
    public void setStoreObserver(IObserver<ModuleConstraintStore> observer) {
        this.observer = observer;
    }
    
    @Override
    public int activeSize() {
        return active.size();
    }
    
    @Override
    public int delayedSize() {
        return stuckOnModule.size() + stuckOnVar.size() + stuckOnEdge.size();
    }
    
    @Override
    public void addAll(Iterable<? extends IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            active.add(constraint);
        }
    }
    
    @Override
    public void add(IConstraint constraint) {
        active.add(constraint);
    }
    
    @Override
    public IConstraint remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delay(IConstraint constraint, Delay delay) {
        final Delayed delayed = new Delayed(constraint);
        try {
            if (!delay.vars().isEmpty()) {
                TDebug.DEV_OUT.info("delayed {} on vars {}", constraint, delay.vars());
                for (ITermVar var : delay.vars()) {
                    stuckOnVar.put(var, delayed);
                    resolveStuckOnOtherModule(var, constraint, TDebug.DEV_OUT);
                }
            } else if (!delay.criticalEdges().isEmpty()) {
                TDebug.DEV_OUT.info("delayed {} on critical edges {}", constraint, delay.criticalEdges());
                for (CriticalEdge edge : delay.criticalEdges()) {
                    stuckOnEdge.put(edge, delayed);
                    registerAsObserver(edge, TDebug.DEV_OUT);
                }
            } else if (delay.module() != null) {
                TDebug.DEV_OUT.warn("delayed {} on module {}", constraint, delay.module());
                stuckOnModule.put(delay.module(), delayed);
            } else {
                throw new IllegalArgumentException("delayed for no apparent reason");
            }
        } finally {
            if (delay.getLockManager() != null) {
                delay.getLockManager().releaseAll();
            }
        }
    }
    
    /**
     * The solver is guaranteed to be done if it has no more constraints.
     * It should be able to be done even if there are child solvers still solving.
     * 
     * <p>NOTE: This method is not concurrency safe! The result is only correct if it is requested
     * by the thread currently executing the solver, or if there is no thread currently executing
     * the solver. Otherwise, there is a small window where a true result does not actually mean
     * that the solver is done.
     * 
     * @return
     *      true if this solver is done, false otherwise
     */
    public boolean isDone() {
        return activeSize() + delayedSize() == 0;
    }
    
    /**
     * @return
     *      true if this store can make progress
     */
    public boolean canProgress() {
        return activeSize() > 0;
    }
    
    @Override
    public void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        if (Iterables.isEmpty(vars)) return;
        
        synchronized (variableLock) {
            for (ITermVar termVar : vars) {
                activateFromVar(termVar, debug);
            }
        }
        
        Set<ModuleConstraintStore> stores = new HashSet<>();
        synchronized (varObservers) {
            //Activate all observers
            for (ITermVar termVar : vars) {
                for (ModuleConstraintStore store : varObservers.removeAll(termVar)) {
                    //We first need to active and then, if it is likely that the module is currently not solving, we send a notification
                    if (STORE_DEBUG) System.err.println(owner + ": Delegating activation of variable " + termVar + " to " + store.owner);
                    store.activateFromVar(termVar, debug); //Activate but don't propagate
                    //Only notify if it is currently not doing anything (probably)
                    if (store.activeSize() == 1) stores.add(store);
                }
            }
            
            //Notify each store only once
            for (ModuleConstraintStore store : stores) {
                if (store.observer != null) store.observer.notify(this);
            }
        }
    }
    
    public void activateFromVar(ITermVar var, IDebugContext debug) {
        Collection<Delayed> activated;
        synchronized (stuckOnVar) {
            activated = stuckOnVar.removeAll(var);
            stuckOnVar.values().removeAll(activated);
        }
        
        for (Delayed delayed : activated) {
            if (delayed.activate()) {
                final IConstraint constraint = delayed.constraint;
                debug.info("activating {}", constraint);
                add(constraint);
            }
        }
    }
    
    @Override
    public void activateFromEdges(Iterable<? extends CriticalEdge> edges, IDebugContext debug) {
        if (Iterables.isEmpty(edges)) return;
        
        for (CriticalEdge edge : edges) {
            activateFromEdge(edge, debug, false);
        }
        
        Set<ModuleConstraintStore> stores = new HashSet<>();
        
        //Activate all observers
        for (CriticalEdge edge : edges) {
            final Set<ModuleConstraintStore> observers;
            synchronized (edgeObservers) {
                observers = edgeObservers.removeAll(edge);
            }
            
            for (ModuleConstraintStore store : observers) {
                //We first need to active and then, if it is likely that the module is currently not solving, we send a notification
                if (STORE_DEBUG) System.err.println(owner + ": Delegating activation of edge " + edge + " to " + store.owner);
                
                store.activateFromEdge(edge, debug, false); //Activate but don't propagate
                
                //Only notify if it is currently not doing anything (probably)
                final IObserver<ModuleConstraintStore> observer = store.observer;
                if (observer != null && store.activeSize() == 1) {
                    stores.add(store);
                }
            }
        }
        
        //Notify each store only once
        for (ModuleConstraintStore store : stores) {
            if (store.observer != null) store.observer.notify(this);
        }
    }
    
    public void activateFromEdge(CriticalEdge edge, IDebugContext debug, boolean propagate) {
        final Collection<Delayed> activated;
        synchronized (stuckOnEdge) {
            activated = stuckOnEdge.removeAll(edge);
            stuckOnEdge.values().removeAll(activated);
        }
        
        debug.info("activating edge {}", edge);
        for(Delayed delayed : activated) {
            if(delayed.activate()) {
                final IConstraint constraint = delayed.constraint;
                debug.info("activating {}", constraint);
                add(constraint);
            }
        }
        
        //If no propagation is necessary, we can stop here
        if (!propagate) return;
        
        final Set<ModuleConstraintStore> observers;
        synchronized (edgeObservers) {
            observers = edgeObservers.removeAll(edge);
        }
        
        //Activate all observers
        for (ModuleConstraintStore store : observers) {
            //We first need to active and then, if it is likely that the module is currently not solving, we send a notification
            if (STORE_DEBUG) System.err.println(owner + ": Delegating activation of edge " + edge + " to " + store.owner);
            
            store.activateFromEdge(edge, debug, false); //Activate but don't propagate (it is impossible to have effect)
            
            //Only notify if it is currently not doing anything (probably)
            final IObserver<ModuleConstraintStore> observer = store.observer;
            if (observer != null && store.activeSize() == 1) {
                observer.notify(this);
            }
        }
    }
    
    public void registerObserver(CriticalEdge edge, ModuleConstraintStore store, IDebugContext debug) {
        synchronized (edgeObservers) {
            edgeObservers.put(edge, store);
        }
    }
    
    public void registerObserver(ITermVar termVar, ModuleConstraintStore store, IDebugContext debug) {
        synchronized (varObservers) {
            varObservers.put(termVar, store);
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
                progress = false;
                return active.poll();
            }
        }
        
        //we are stuck (potentially waiting for another solver)
        return constraint;
    }
    
    /**
     * Gets an active constraint from this store.
     * 
     * @param debug
     *      the debug context
     * 
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
                final Delayed delayed = new Delayed(constraint);
                try {
                    if (!d.vars().isEmpty()) {
                        debug.info("delayed {} on vars {}", constraint, d.vars());
                        for (ITermVar var : d.vars()) {
                            stuckOnVar.put(var, delayed);
                            resolveStuckOnOtherModule(var, constraint, debug);
                        }
                    } else if (!d.criticalEdges().isEmpty()) {
                        debug.info("delayed {} on critical edges {}", constraint, d.criticalEdges());
                        for (CriticalEdge edge : d.criticalEdges()) {
                            stuckOnEdge.put(edge, delayed);
                            registerAsObserver(edge, debug);
                        }
                    } else if (d.module() != null){
                        debug.warn("delayed {} on module {}", constraint, d.module());
                        stuckOnModule.put(d.module(), delayed);
                    } else {
                        throw new IllegalArgumentException("delayed for no apparent reason");
                    }
                } finally {
                    if (d.getLockManager() != null) {
                        d.getLockManager().releaseAll();
                    }
                }
            }

            @Override
            public void remove() {
                synchronized (ModuleConstraintStore.this) {
                    progress = true;
                }
            }
        };
    }
    
    private void resolveStuckOnOtherModule(ITermVar termVar, IConstraint constraint, IDebugContext debug) {
        //Not stuck on another module
        final IModule varOwner;
        if (this.owner.equals(termVar.getResource()) || (varOwner = Vars.getOwnerUnchecked(termVar)) == null) return;
        
        ModuleConstraintStore varStore = varOwner.getCurrentState().solver().getStore();
        
        synchronized (varStore.variableLock) {
            IUnifier.Immutable unifier = varOwner.getCurrentState().unifier();
            //Cannot be removed because of locking stuff
            if (!unifier.isGround(termVar)) {
                if (STORE_DEBUG) System.err.println(owner + ": Registering as observer on " + varOwner + " for " + termVar);
                registerAsObserver(termVar, debug);
            } else {
                System.err.println(termVar + " is ground according to the unifier!");
                active.add(constraint);
            }
        }
    }
    
    /**
     * Registers this store as an observer of the given critical edge.
     * The registration is made with the store of the solver of the owner of the scope of the given
     * critical edge.
     * 
     * @param edge
     *      the edge
     * @param debug
     *      the debug context
     */
    private void registerAsObserver(CriticalEdge edge, IDebugContext debug) {
        IModule owner = getEdgeCause(edge);
        if (owner == null) throw new IllegalStateException("Encountered edge without being able to determine the owner!");
        
        //A module doesn't have to register on itself
        if (this.owner.equals(owner.getId())) return;

        //TODO Static state access
        ModuleConstraintStore store = owner.getCurrentState().solver().getStore();
        assert store != this : "FATAL: Current states are messed up in the context: owner inconsistency!";
        
        debug.info("Registering as observer on {}, waiting on edge {}", owner, edge);
        store.registerObserver(edge, this, debug);
    }
    
    /**
     * @param edge
     *      the edge
     * 
     * @return
     *      the cause of the edge
     */
    private IModule getEdgeCause(CriticalEdge edge) {
        return M.cases(
                Scope.matcher().map(Scope::getResource),
                M.var().map(ITermVar::getResource)
            ).match(edge.scope())
             .map(s -> SolverContext.context().getModuleUnchecked(s))
             .orElse(null);
    }
    
    /**
     * Registers this store as an observer of the given term variable.
     * The registration is made with the store of the solver of the owner of the given term
     * variable.
     * 
     * @param termVar
     *      the term variable
     * @param context
     *      the solver context
     * @param debug
     *      the debug context
     */
    private void registerAsObserver(ITermVar termVar, IDebugContext debug) {
        IModule varOwner = Vars.getOwnerUnchecked(termVar);
        //A module doesn't have to register on itself
        if (this.owner.equals(varOwner.getId())) return;

        //TODO Static state access
        ModuleConstraintStore store = varOwner.getCurrentState().solver().getStore();
        assert store != this : "FATAL: Current states are messed up in the context: owner inconsistency!";
        
        debug.info("Registering as observer on {}, waiting on var {}", varOwner, termVar);
        store.registerObserver(termVar, this, debug);
    }
    
    @Override
    public Map<IConstraint, Delay> delayed() {
        final Multimap<IConstraint, ITermVar> varStuck = HashMultimap.create();
        stuckOnVar.entries().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> varStuck.put(e.getValue().constraint, e.getKey()));

        final Multimap<IConstraint, CriticalEdge> edgeStuck = HashMultimap.create();
        stuckOnEdge.entries().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> edgeStuck.put(e.getValue().constraint, e.getKey()));

        final Map<IConstraint, String> moduleStuck = new HashMap<>();
        stuckOnModule.entries().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> moduleStuck.put(e.getValue().constraint, e.getKey()));
        
        final Set<IConstraint> stuck = new HashSet<>();
        stuck.addAll(varStuck.keys());
        stuck.addAll(edgeStuck.keys());
        stuck.addAll(moduleStuck.keySet());

        final Builder<IConstraint, Delay> delayed = ImmutableMap.builder();
        stuck.stream().forEach(c -> delayed.put(c, new Delay(varStuck.get(c), edgeStuck.get(c), moduleStuck.get(c), null)));
        return delayed.build();
    }
    
    @Override
    public String toString() {
        return "ModuleConstraintStore<" + owner + ">";
    }
    
    private static class Delayed {

        public final IConstraint constraint;

        private boolean activated = false;

        public Delayed(IConstraint constraint) {
            this.constraint = constraint;
        }

        public boolean activate() {
            if(activated) {
                return false;
            } else {
                activated = true;
                return true;
            }
        }

    }
}

