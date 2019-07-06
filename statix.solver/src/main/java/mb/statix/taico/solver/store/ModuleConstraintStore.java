package mb.statix.taico.solver.store;

import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.taico.util.TDebug.STORE_DEBUG;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
import mb.statix.taico.solver.completeness.RedirectingIncrementalCompleteness;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.util.TDebug;
import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.Vars;

public class ModuleConstraintStore implements IConstraintStore {
    private final String owner;
    private final Deque<IConstraint> active;
    private final Multimap<ITermVar, Delayed> stuckOnVar;
    private final Multimap<CriticalEdge, Delayed> stuckOnEdge;
    private final Multimap<String, Delayed> stuckOnModule;
    
    private final SetMultimap<ITermVar, ModuleConstraintStore> varObservers;
    
    private volatile IObserver<ModuleConstraintStore> observer;
    
    private final Object variableLock = new Object();
    private volatile boolean externalMode = false;
    
    public ModuleConstraintStore(String owner, Iterable<? extends IConstraint> constraints, IDebugContext debug) {
        this.owner = owner;
        this.active = new LinkedList<>(); //TODO For concurrency, ConcurrentLinkedDeque?
        this.stuckOnVar = MultimapBuilder.hashKeys().hashSetValues().build();
        this.stuckOnEdge = MultimapBuilder.hashKeys().hashSetValues().build();
        this.stuckOnModule = MultimapBuilder.hashKeys().hashSetValues().build();
        this.varObservers = MultimapBuilder.hashKeys().hashSetValues().build();
        addAll(constraints);
    }
    
    /**
     * Enables external mode. External mode allows constraints to be added to this store
     * externally. Until external mode is disabled, {@link #isDone()} reports false.
     */
    public void enableExternalMode() {
        externalMode = true;
    }
    
    /**
     * Disables external mode.
     * 
     * @see #enableExternalMode()
     */
    public void disableExternalMode() {
        externalMode = false;
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
        synchronized (active) {
            for (IConstraint constraint : constraints) {
                active.push(constraint);
            }
        }
    }
    
    @Override
    public void add(IConstraint constraint) {
        synchronized (active) {
            active.push(constraint);
        }
    }
    
    /**
     * Adds the given constraint to this store externally.
     * 
     * @param constraint
     *      the constraint
     * 
     * @throws IllegalStateException
     *      If externalMode is not enabled.
     * 
     * @see #enableExternalMode()
     */
    public void externalAdd(IConstraint constraint) {
        if (!externalMode) throw new IllegalStateException("Adding external constraints is only allowed if external mode is activated.");
        
        synchronized (active) {
            active.push(constraint);
        }
        notifyObserver();
    }
    
    @Override
    public IConstraint remove() {
        synchronized (active) {
            return active.poll();
        }
    }

    @Override
    public void delay(IConstraint constraint, Delay delay) {
        final Delayed delayed = new Delayed(constraint);
        try {
            if (!delay.vars().isEmpty()) {
                TDebug.DEV_OUT.info("delayed {} on vars {}", constraint, delay.vars());
                for (ITermVar var : delay.vars()) {
                    synchronized (stuckOnVar) {
                        stuckOnVar.put(var, delayed);
                    }
                    resolveStuckOnOtherModule(var, constraint, TDebug.DEV_OUT);
                }
            } else if (!delay.criticalEdges().isEmpty()) {
                TDebug.DEV_OUT.info("delayed {} on critical edges {}", constraint, delay.criticalEdges());
                for (CriticalEdge edge : delay.criticalEdges()) {
                    synchronized (stuckOnEdge) {
                        stuckOnEdge.put(edge, delayed);
                    }
                    registerAsObserver(edge, TDebug.DEV_OUT);
                }
            } else if (delay.module() != null) {
                TDebug.DEV_OUT.warn("delayed {} on module {}", constraint, delay.module());
                synchronized (stuckOnModule) {
                    stuckOnModule.put(delay.module(), delayed);
                }
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
        
        propagateVariableActivation(vars, debug);
    }

    private void propagateVariableActivation(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        Set<ModuleConstraintStore> stores = new HashSet<>();
        //Activate all observers
        for (ITermVar termVar : vars) {
            Collection<ModuleConstraintStore> observers;
            synchronized (varObservers) {
                observers = varObservers.removeAll(termVar);
            }
            for (ModuleConstraintStore store : observers) {
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
    
    public boolean activateFromVar(ITermVar var, IDebugContext debug) {
        Collection<Delayed> activated;
        synchronized (stuckOnVar) {
            activated = stuckOnVar.removeAll(var);
            stuckOnVar.values().removeAll(activated);
        }
        
        boolean tbr = false;
        for (Delayed delayed : activated) {
            if (delayed.activate()) {
                final IConstraint constraint = delayed.constraint;
                debug.info("activating {}", constraint);
                add(constraint);
                tbr = true;
            }
        }
        return tbr;
    }
    
    public void externalActivateFromVar(ITermVar var, IDebugContext debug) {
        if (activateFromVar(var, debug)) {
            notifyObserver();
        }
    }
    
    @Override
    public void activateFromEdges(Iterable<? extends CriticalEdge> edges, IDebugContext debug) {
        for (CriticalEdge edge : edges) {
            activateFromEdge(edge, debug);
        }
    }
    
    public boolean activateFromEdge(CriticalEdge edge, IDebugContext debug) {
        final Collection<Delayed> activated;
        synchronized (stuckOnEdge) {
            activated = stuckOnEdge.removeAll(edge);
            stuckOnEdge.values().removeAll(activated);
        }
        
        boolean tbr = false;
        debug.info("activating edge {}", edge);
        for(Delayed delayed : activated) {
            if(delayed.activate()) {
                final IConstraint constraint = delayed.constraint;
                debug.info("activating {}", constraint);
                add(constraint);
                tbr = true;
            }
        }
        
        return tbr;
    }
    
    /**
     * Method that is to be called when an edge for a scope owned by a different module is
     * activated.
     * 
     * @param edge
     *      the edge to activate
     * @param debug
     *      the debug
     */
    public void externalActivateFromEdge(CriticalEdge edge, IDebugContext debug) {
        if (activateFromEdge(edge, debug)) {
            notifyObserver();
        }
    }
    
    public void activateFromModules(IDebugContext debug) {
        final Collection<Delayed> activated;
        synchronized (stuckOnModule) {
            activated = new HashSet<>(stuckOnModule.values());
            stuckOnModule.clear();
        }
        
        debug.info("activating all modules");
        for(Delayed delayed : activated) {
            if(delayed.activate()) {
                final IConstraint constraint = delayed.constraint;
                debug.info("activating {}", constraint);
                add(constraint);
            }
        }
    }
    
    public boolean activateFromModule(String module, IDebugContext debug) {
        final Collection<Delayed> activated;
        synchronized (stuckOnModule) {
            activated = stuckOnModule.removeAll(module);
            stuckOnModule.values().removeAll(activated);
        }
        
        boolean tbr = false;
        debug.info("activating module {}", module);
        for(Delayed delayed : activated) {
            if(delayed.activate()) {
                final IConstraint constraint = delayed.constraint;
                debug.info("activating {}", constraint);
                add(constraint);
                tbr = true;
            }
        }
        
        return tbr;
    }
    
    /**
     * Method that is to be called when a module is being activated by a different module than
     * the owner of this store.
     * 
     * @param module
     *      the module to activate
     * @param debug
     *      the debug
     */
    public void externalActivateFromModule(String module, IDebugContext debug) {
        if (activateFromModule(module, debug)) {
            notifyObserver();
        }
    }

    /**
     * Notifies the observer if there is any.
     */
    private void notifyObserver() {
        final IObserver<ModuleConstraintStore> observer = this.observer;
        if (observer != null) {
            observer.notify(this);
        }
    }
    
    public void registerObserver(ITermVar termVar, ModuleConstraintStore store, IDebugContext debug) {
        synchronized (varObservers) {
            varObservers.put(termVar, store);
        }
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
     * The registration is made with the completeness of the solver of the owner of the scope of
     * the given critical edge.
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
        if (!TOverrides.USE_OBSERVER_MECHANISM_FOR_SELF && this.owner.equals(owner.getId())) return;
        
        final IMState ownerState = owner.getCurrentState();
        RedirectingIncrementalCompleteness completeness = ownerState.solver().getCompleteness();
        debug.info("Registering as observer on {}, waiting on edge {}", owner, edge);
        completeness.registerObserver(edge.scope(), edge.label(), ownerState.unifier(), e -> externalActivateFromEdge(e, debug));
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
        final Multimap<IConstraint, ITermVar> varStuck = MultimapBuilder.hashKeys().hashSetValues().build();
        stuckOnVar.entries().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> varStuck.put(e.getValue().constraint, e.getKey()));

        final Multimap<IConstraint, CriticalEdge> edgeStuck = MultimapBuilder.hashKeys().hashSetValues().build();
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
    
    /**
     * @return
     *      all the constraints remaining in the store
     */
    public Set<IConstraint> getAllRemainingConstraints() {
        Set<IConstraint> tbr = new HashSet<>();
        tbr.addAll(active);
        for (Delayed d : stuckOnEdge.values()) tbr.add(d.constraint);
        for (Delayed d : stuckOnVar.values()) tbr.add(d.constraint);
        for (Delayed d : stuckOnModule.values()) tbr.add(d.constraint);
        return tbr;
    }
    
    @Override
    public String toString() {
        return "ModuleConstraintStore<" + owner + ">";
    }
    
    private static class Delayed {

        public final IConstraint constraint;

        private volatile boolean activated = false;

        public Delayed(IConstraint constraint) {
            this.constraint = constraint;
        }

        public synchronized boolean activate() {
            if(activated) {
                return false;
            } else {
                activated = true;
                return true;
            }
        }

    }
}

