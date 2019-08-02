package mb.statix.taico.solver.completeness;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.spec.Spec;
import mb.statix.taico.observers.EdgeCompleteManager;
import mb.statix.taico.observers.EdgeCompleteObserver;
import mb.statix.taico.solver.Context;
import mb.statix.taico.util.Scopes;

public class RedirectingIncrementalCompleteness extends IncrementalCompleteness implements EdgeCompleteManager {
    private final String owner;
    
    private final SetMultimap<CriticalEdge, EdgeCompleteObserver> observers = MultimapBuilder.hashKeys().hashSetValues().build();
    
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
        return getTarget(scope)._isComplete(scope, label);
    }

    @Override
    public void add(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> {
                getTarget(scope).add(scope, label);
            });
        });
    }

    @Override
    public void remove(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> {
                getTarget(scope).remove(scope, label);
            });
        });
    }
    
    @Override
    public void update(ITermVar var, IUnifier unifier) {
        final Multiset<ITerm> labels = incomplete.remove(var);
        if(labels != null) {
            getVarOrScope(var, unifier).ifPresent(scope -> {
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
        
        return Context.context().getModuleUnchecked(owner).getCurrentState().solver().getCompleteness();
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
        return super._isComplete(scope, label);
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "RedirectingIncrementalCompleteness<" + owner + ">";
    }
}
