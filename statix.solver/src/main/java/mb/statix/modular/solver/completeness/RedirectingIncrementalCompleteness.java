package mb.statix.modular.solver.completeness;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.modular.observers.EdgeCompleteManager;
import mb.statix.modular.observers.EdgeCompleteObserver;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.util.Scopes;
import mb.statix.modular.util.TOverrides;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.spec.Spec;

public class RedirectingIncrementalCompleteness extends IncrementalCompleteness implements EdgeCompleteManager {
    public static final Map<String, RedirectingIncrementalCompleteness> RECOVERY = TOverrides.hashMap();
    private final String owner;
    
    private SetMultimap<CriticalEdge, EdgeCompleteObserver> observers = MultimapBuilder.hashKeys().hashSetValues().build();
    private boolean delayMode;
    
    public RedirectingIncrementalCompleteness(String owner, Spec spec) {
        super(spec);
        this.owner = owner;
        this.edgeCompleteObserver = this::activateObservers;
    }
    
    protected RedirectingIncrementalCompleteness(String owner, Spec spec, Map<ITerm, Multiset<ITerm>> incomplete) {
        super(spec, incomplete);
        this.owner = owner;
        this.edgeCompleteObserver = this::activateObservers;
    }

    @Override
    public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        final RedirectingIncrementalCompleteness target = getTarget(scope);
        if (target == this && delayMode) return false;
        return target._isComplete(scope, label);
    }

    @Override
    public void add(IConstraint constraint, IUnifier unifier) {
        final IUnifier uunifier = unifier.unrestricted();
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, uunifier).ifPresent(scope -> {
                getTarget(scope).add(scope, label);
            });
        });
    }

    @Override
    public void remove(IConstraint constraint, IUnifier unifier) {
        final IUnifier uunifier = unifier.unrestricted();
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, uunifier).ifPresent(scope -> {
                getTarget(scope).remove(scope, label);
            });
        });
    }
    
    @Override
    public void update(ITermVar var, IUnifier unifier) {
        final IUnifier uunifier = unifier.unrestricted();
        final Multiset<ITerm> labels = incomplete.remove(var);
        if(labels != null) {
            getVarOrScope(var, uunifier).ifPresent(scope -> {
                //TODO TAICO: Remove this check
                //TODO TAICO: This is a temporary check to assert the variable is equal
                if (scope instanceof Scope) {
                    if (!Scopes.getOwnerUnchecked(scope).getId().equals(var.getResource())) {
                        throw new IllegalStateException("Scope owner should be equal");
                    }
                }
                
                incomplete.computeIfAbsent(scope, s -> createMultiset()).addAll(labels);
                //If the variable didn't change, then there is nothing we need to do.
                if (var.equals(scope)) return;
                
                //TODO Optimization
                //TODO I am not sure if this can even occur, since variables might only come from the own module.
                
                SetMultimap<CriticalEdge, EdgeCompleteObserver> substitution = HashMultimap.create();
                synchronized (observers) {
                    Iterator<Entry<CriticalEdge, EdgeCompleteObserver>> it = observers.entries().iterator();
                    while (it.hasNext()) {
                        Entry<CriticalEdge, EdgeCompleteObserver> e = it.next();
                        if (!e.getKey().scope().equals(var)) continue;
                        
                        System.err.println("Variable substitution required on observers!!!!");
                        it.remove();
                        substitution.put(CriticalEdge.of(scope, e.getKey().label()), e.getValue());
                    }
                    observers.putAll(substitution);
                }
            });
        }
    }
    
    @Override
    public RedirectingIncrementalCompleteness getTarget(ITerm term) {
        final String owner;
        if (term instanceof ITermVar) {
            owner = ((ITermVar) term).getResource();
        } else if (term instanceof Scope) {
            owner = ((Scope) term).getResource();
        } else {
            throw new IllegalArgumentException("Expected variable or scope, but was " + term);
        }
        
        if (this.owner.equals(owner)) return this;
        
        //TODO IMPORTANT TEMPORARY CHECK
        ModuleSolver solver = Context.context().getSolver(owner);
        if (solver == null) {
            System.err.println("Cannot find the owner of the given term: owner=" + owner + " requested by " + this.owner + ", term=" + term);
            return getOrCreateRecovery(owner);
        }
        return solver.getCompleteness();
    }

    private static RedirectingIncrementalCompleteness getOrCreateRecovery(final String owner) {
        return RECOVERY.computeIfAbsent(owner, o -> {
            RedirectingIncrementalCompleteness ric = new RedirectingIncrementalCompleteness(owner, Context.context().getSpec());
            ric.switchDelayMode(true);
            return ric;
        });
    }
    
    // --------------------------------------------------------------------------------------------
    // Other
    // --------------------------------------------------------------------------------------------
    
    /**
     * Adds incompleteness based on the remaining constraints in the given result.
     * 
     * @param result
     *      the solver result
     */
    public void fillFromResult(MSolverResult result) {
        for (IConstraint constraint : result.delays().keySet()) {
            add(constraint, result.unifier());
        }
        
        for (IConstraint constraint : result.errors()) {
            add(constraint, result.unifier());
        }
    }
    
    public boolean isInDelayMode() {
        return delayMode;
    }
    
    /**
     * Sets the delay mode to the given value. If delay mode is enabled, this completeness will
     * report that all its scopes/edges are incomplete, but will still redirect to other
     * completenesses whenever it should.
     * 
     * If delay mode is deactivated, all observers are checked to see if they should be activated,
     * and are activated if necessary.
     * 
     * @param delayMode
     *      the new value for the delay mode
     */
    public void switchDelayMode(boolean delayMode) {
        if (this.delayMode == delayMode) return;
        
        this.delayMode = delayMode;
        if (delayMode) return;
        
        synchronized (observers) {
            for (CriticalEdge ce : observers.keySet()) {
                if (!super._isComplete((Scope) ce.scope(), ce.label())) continue;
                
                for (EdgeCompleteObserver observer : observers.removeAll(ce)) {
                    observer.accept(ce);
                }
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Observers
    // --------------------------------------------------------------------------------------------
    
    @Override
    public SetMultimap<CriticalEdge, EdgeCompleteObserver> observers() {
        return observers;
    }

    @Override
    public boolean alreadyResolved(Scope scope, ITerm label) {
        return !delayMode && super._isComplete(scope, label);
    }
    
    /**
     * Transfers the information in this completeness to the given completeness.
     * This method assumes that the transfer is to a completeness that is more incomplete than the
     * current completeness.
     * 
     * The observers are transferred first.
     * The given constraints are removed from this completeness before the incomplete data is
     * transferred.
     * 
     * @param completeness
     *      the completeness to transfer to
     */
    @Deprecated
    public void transfer(RedirectingIncrementalCompleteness completeness, Set<IConstraint> delayed, IUnifier unifier) {
        //Swap the observers with an unmodifiable map
        SetMultimap<CriticalEdge, EdgeCompleteObserver> copy;
        SetMultimap<CriticalEdge, EdgeCompleteObserver> replacement =
                MultimapBuilder.hashKeys().hashSetValues().build();
        
        //TODO Concurrency, the observers should not be modified after this point
        synchronized (observers) {
            copy = observers;
            observers = replacement;
        }
        
        //Remove all the delayed constraints that are passed to us to "clear" this completeness
        //from its owners constraints, but to free it otherwise.
        removeAll(delayed, unifier);
        completeness.incomplete.putAll(this.incomplete);
        
        for (Entry<CriticalEdge, EdgeCompleteObserver> entry : copy.entries()) {
            CriticalEdge ce = entry.getKey();
            if (!completeness._registerObserver((Scope) ce.scope(), ce.label(), entry.getValue())) {
                System.err.println("Transfer resulted in observers getting triggered, this should not occur!");
                throw new IllegalStateException("Transfer of completeness immediately activated ");
            }
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "RedirectingIncrementalCompleteness<" + owner + ">";
    }
}
