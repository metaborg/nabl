package mb.statix.taico.solver.store;

import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.taico.util.TDebug.STORE_DEBUG;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

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
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.completeness.RedirectingIncrementalCompleteness;
import mb.statix.taico.solver.state.DelegatingMState;
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
    
    private volatile boolean externalMode = false;
    
    public ModuleConstraintStore(String owner, Iterable<? extends IConstraint> constraints, IDebugContext debug) {
        this.owner = owner;
        this.active = new LinkedList<>(); //TODO For concurrency, ConcurrentLinkedDeque?
        this.stuckOnVar = MultimapBuilder.hashKeys().arrayListValues().build();
        this.stuckOnEdge = MultimapBuilder.hashKeys().arrayListValues().build();
        this.stuckOnModule = MultimapBuilder.hashKeys().arrayListValues().build();
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
    @Deprecated
    public void delay(IConstraint constraint, Delay delay) {
        final Delayed delayed = new Delayed(constraint);
        if (!delay.vars().isEmpty()) {
            TDebug.DEV_OUT.info("delayed {} on vars {}", constraint, delay.vars());
            for (ITermVar var : delay.vars()) {
                synchronized (stuckOnVar) {
                    stuckOnVar.put(var, delayed);
                }
                if (!registerAsObserver(var, TDebug.DEV_OUT, null)) break;
            }
        } else if (!delay.criticalEdges().isEmpty()) {
            TDebug.DEV_OUT.info("delayed {} on critical edges {}", constraint, delay.criticalEdges());
            for (CriticalEdge edge : delay.criticalEdges()) {
                synchronized (stuckOnEdge) {
                    stuckOnEdge.put(edge, delayed);
                }
                if (!registerAsObserver(edge, TDebug.DEV_OUT, null)) break;
            }
        } else if (delay.module() != null) {
            TDebug.DEV_OUT.warn("delayed {} on module {}", constraint, delay.module());
            synchronized (stuckOnModule) {
                stuckOnModule.put(delay.module(), delayed);
            }
        } else {
            throw new IllegalArgumentException("delayed for no apparent reason");
        }
    }
    
    public void delay(IConstraint constraint, Delay delay, IMState state) {
        final Delayed delayed = new Delayed(constraint);
        if (!delay.vars().isEmpty()) {
            TDebug.DEV_OUT.info("delayed {} on vars {}", constraint, delay.vars());
            for (ITermVar var : delay.vars()) {
                synchronized (stuckOnVar) {
                    stuckOnVar.put(var, delayed);
                }
                if (!registerAsObserver(var, TDebug.DEV_OUT, state)) break;
            }
        } else if (!delay.criticalEdges().isEmpty()) {
            TDebug.DEV_OUT.info("delayed {} on critical edges {}", constraint, delay.criticalEdges());
            for (CriticalEdge edge : delay.criticalEdges()) {
                synchronized (stuckOnEdge) {
                    stuckOnEdge.put(edge, delayed);
                }
                if (!registerAsObserver(edge, TDebug.DEV_OUT, state)) break;
            }
        } else if (delay.module() != null) {
            TDebug.DEV_OUT.warn("delayed {} on module {}", constraint, delay.module());
            synchronized (stuckOnModule) {
                stuckOnModule.put(delay.module(), delayed);
            }
        } else {
            throw new IllegalArgumentException("delayed for no apparent reason");
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
        
        for (ITermVar termVar : vars) {
            activateFromVar(termVar, debug);
        }
        
        propagateVariableActivation(vars, debug);
    }

    private void propagateVariableActivation(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        Set<ModuleConstraintStore> stores = new HashSet<>();
        //Activate all observers
        int counter = 0;
        for (ITermVar termVar : vars) {
            Collection<ModuleConstraintStore> observers;
            synchronized (varObservers) {
                observers = varObservers.removeAll(termVar);
            }
            
            for (ModuleConstraintStore store : observers) {
                //We first need to active and then, if it is likely that the module is currently not solving, we send a notification
                if (STORE_DEBUG) System.err.println(owner + ": Delegating activation of variable " + termVar + " to " + store.owner);
                
                if (!store.activateFromVar(termVar, debug)) continue; //Activate but don't propagate
                
                //Only send the event near the end
                if (!stores.add(store)) counter++;
            }
        }

        //TODO This checks if the event sending optimization is even worth it in these cases.
        if (counter > 0) System.out.println("Caching stores for variable activation relieved " + counter + " notifications");
        
        //Notify each store only once
        for (ModuleConstraintStore store : stores) {
            store.notifyObserver();
        }
    }
    
    public boolean activateFromVar(ITermVar var, IDebugContext debug) {
        //We do not need to lock the variable lock here, since the unifier 
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
    
    // --------------------------------------------------------------------------------------------
    // Observers
    // --------------------------------------------------------------------------------------------

    /**
     * Notifies the observer if there is any.
     */
    public void notifyObserver() {
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
    
    /**
     * Registers this store as an observer of the given variable. If the variable was activated
     * before the registration completed, this method instead triggers the variable activation
     * event immediately.
     * If the owner of the given variable is the owner of this store, or if the owner is
     * unknown, this method does nothing.
     * <p>
     * In particular, after obtaining the variable registration lock of the owner of the variable,
     * this method checks if the variable has been resolved in the meantime. If it has not been
     * resolved, this store is registered as observer for the given variable. Otherwise, the given
     * variable is resolved immediately.
     * 
     * @param termVar
     *      the variable
     * @param debug
     *      the debug
     * @param state
     *      the state of the owner of this constraint store
     * 
     * @return
     *      false if the var was immediately activated, true otherwise
     */
    private boolean registerAsObserver(ITermVar termVar, IDebugContext debug, @Nullable IMState state) {
        final IModule varOwner;
        if (this.owner.equals(termVar.getResource()) || (varOwner = Vars.getOwnerUnchecked(termVar)) == null) return true;
        
        if (state == null) state = Context.context().getState(this.owner);
        final IMState varState = varOwner.getCurrentState();
        final ModuleConstraintStore varStore = varState.solver().getStore();
        
        //Before checking, ensure that the other side cannot notify observers causing us to miss the event
        synchronized (varStore.varObservers) {
            IUnifier.Immutable unifier = state.unifier();
            //If we are running concurrently, check if the variable was resolved between our stuck check and the registration.
            //TODO Optimization check if we are running concurrently
            //TODO Could we get a module delay exception here?
            if (!unifier.isGround(termVar)) {
                debug.info("Registering {} as observer on {}, waiting on var {}", owner, varOwner, termVar);
                varStore.registerObserver(termVar, this, debug);
                return true;
            }
        }
        
        //The term is already ground, resolve immediately.
        activateFromVar(termVar, debug);
        return false;
    }
    
    /**
     * Registers this store as an observer of the given critical edge. If the edge was already
     * resolved before the registration completed, the edge activation event is triggered
     * immediately.
     * <p>
     * If the owner of the given edge is the owner of this store and
     * {@link TOverrides#USE_OBSERVER_MECHANISM_FOR_SELF} is false, then this method does nothing.
     * 
     * @param edge
     *      the edge
     * @param debug
     *      the debug context
     * @param state
     *      the state of the owner of this constraint store
     * 
     * @return
     *      false if the edge was immediately activated, true otherwise
     */
    private boolean registerAsObserver(CriticalEdge edge, IDebugContext debug, @Nullable IMState state) {
        //TODO IMPORTANT We do not want to reactivate separate solvers after they have reported their result
        IModule owner = getEdgeCause(edge);
        if (owner == null) throw new IllegalStateException("Encountered edge without being able to determine the owner!");
        
        //A module doesn't have to register on itself
        if (!TOverrides.USE_OBSERVER_MECHANISM_FOR_SELF && this.owner.equals(owner.getId())) return true;
        
        if (state instanceof DelegatingMState && this.owner.equals(owner.getId())) {
            System.out.println("Running observer mechanism on a separate solver for " + this.owner);
        } else if (state instanceof DelegatingMState) {
            System.out.println("Using observer mechanism on a separate solver...");
        }
        
        final IMState ownerState = owner.getCurrentState();
        RedirectingIncrementalCompleteness completeness = ownerState.solver().getCompleteness();
        debug.info("Registering {} as observer on {}, waiting on edge {}", this.owner, owner, edge);
        return completeness.registerObserver(edge.scope(), edge.label(), ownerState.unifier(), e -> externalActivateFromEdge(e, debug));
    }
    
    /**
     * Activates all the (variable) observers of this store.
     */
    public void activateAllObservers() {
        for (Entry<ITermVar, ModuleConstraintStore> entry : varObservers.entries()) {
            entry.getValue().activateFromVar(entry.getKey(), TDebug.DEV_OUT);
        }
    }
    
    /**
     * Transfers all the observers to the given store.
     * 
     * @param store
     *      the store
     */
    public void transferAllObservers(ModuleConstraintStore store) {
        store.varObservers.putAll(this.varObservers);
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
             .map(s -> Context.context().getModuleUnchecked(s))
             .orElse(null);
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------
    
    /**
     * Fills the stuck maps from the given result.
     * 
     * @param result
     */
    public void fillFromResult(MSolverResult result) {
        for (Entry<IConstraint, Delay> entry : result.delays().entrySet()) {
            IConstraint constraint = entry.getKey();
            Delay delay = entry.getValue();
            if (!delay.vars().isEmpty()) {
                Delayed delayed = new Delayed(constraint);
                for (ITermVar var : delay.vars()) {
                    stuckOnVar.put(var, delayed);
                }
            } else if (!delay.criticalEdges().isEmpty()) {
                Delayed delayed = new Delayed(constraint);
                for (CriticalEdge edge : delay.criticalEdges()) {
                    stuckOnEdge.put(edge, delayed);
                }
            } else if (delay.module() != null) {
                stuckOnModule.put(delay.module(), new Delayed(constraint));
            } else {
                throw new UnsupportedOperationException("Encountered delay without variables, edges and no module. Cannot determine reason for delay.");
            }
        }
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
        stuck.stream().forEach(c -> delayed.put(c, new Delay(varStuck.get(c), edgeStuck.get(c), moduleStuck.get(c))));
        return delayed.build();
    }
    
    /**
     * @return
     *      all delayed constraints
     */
    public Set<IConstraint> delayedConstraints() {
        final Set<IConstraint> stuck = new HashSet<>();
        for (Delayed d : stuckOnVar.values()) stuck.add(d.constraint);
        for (Delayed d : stuckOnEdge.values()) stuck.add(d.constraint);
        for (Delayed d : stuckOnModule.values()) stuck.add(d.constraint);
        return stuck;
    }
    
    /**
     * Clears all the delayed constraints from this store.
     * 
     * @return
     *      all the constraints that were removed 
     */
    public Set<IConstraint> clearDelays() {
        Set<IConstraint> tbr = getAllRemainingConstraints();
        stuckOnVar.clear();
        stuckOnEdge.clear();
        stuckOnModule.clear();
        return tbr;
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

