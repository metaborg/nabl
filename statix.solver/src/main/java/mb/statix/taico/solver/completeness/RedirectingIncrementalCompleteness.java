package mb.statix.taico.solver.completeness;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.util.Scopes;
import mb.statix.taico.util.TDebug;

public class RedirectingIncrementalCompleteness extends IncrementalCompleteness {
    private final String owner;
    
    private final SetMultimap<CriticalEdge, Consumer<CriticalEdge>> observers = MultimapBuilder.hashKeys().hashSetValues().build();
    
    public RedirectingIncrementalCompleteness(String owner, Spec spec) {
        super(spec);
        this.owner = owner;
    }
    
    protected RedirectingIncrementalCompleteness(String owner, Spec spec, Map<ITerm, Multiset<ITerm>> incomplete) {
        super(spec, incomplete);
        this.owner = owner;
    }
    
    /**
     * Registers the given store as an observer for whenever the given edge is resolved.
     * 
     * If the given critical edge has already been resolved, the given observer is called
     * immediately.
     * 
     * @param edge
     *      the edge to register for
     * @param unifier
     *      the unifier
     * @param observer
     *      the observer to call whenever the edge is resolved
     * 
     * @return
     *      true if registered, false if the critical edge is already resolved and the observer was
     *      called directly
     */
    public boolean registerObserver(CriticalEdge edge, IUnifier unifier, Consumer<CriticalEdge> observer) {
        ITerm scopeTerm = getVarOrScope(edge.scope(), unifier).orElse(null);
        ITerm label = edge.label();
        if (!(scopeTerm instanceof Scope)) {
            throw new UnsupportedOperationException("Cannot observe a critical edge without an actual scope, for " + edge);
            //TODO This might need to be resolved in the own module in this case.
        }
        
        if (!scopeTerm.equals(edge.scope())) {
            edge = CriticalEdge.of(scopeTerm, label);
        }
        
        RedirectingIncrementalCompleteness completeness = getTargetCompleteness(scopeTerm);
        return completeness._registerObserver(edge, (Scope) scopeTerm, observer);
    }
    
    private boolean _registerObserver(CriticalEdge edge, Scope scope, Consumer<CriticalEdge> observer) {
        if (super._isComplete(scope, edge.label())) {
            //Already resolved, immediately activate
            observer.accept(edge);
            return false;
        }
        
        synchronized (observers) {
            observers.put(edge, observer);
        }
        return true;
    }

    @Override
    public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        if (scope.getResource().equals(owner)) {
            return super.isComplete(scope, label, unifier);
        } else {
            return Scopes.getOwnerUnchecked(scope).getCurrentState().solver().getCompleteness().isComplete(scope, label, unifier);
        }
    }

    @Override
    public void add(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> {
                getTargetCompleteness(scope).add(scope, label);
            });
        });
    }

    @Override
    public void remove(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> {
                getTargetCompleteness(scope).remove(scope, label);
            });
        });
    }
    
    @Override
    protected void remove(ITerm scope, ITerm label) {
        if (TDebug.COMPLETENESS) System.out.println("Removing " + scope + "-" + label + " from incomplete in " + this);
        final Multiset<ITerm> labels = incomplete.computeIfAbsent(scope, s -> createMultiset());
        if (labels.remove(label) && labels.isEmpty()) {
            activateObservers(CriticalEdge.of(scope, label));
        }
    }
    
    /**
     * Activates all the observers for the given critical edge.
     * 
     * @param edge
     *      the edge to activate
     */
    private void activateObservers(CriticalEdge edge) {
        if (TDebug.COMPLETENESS) System.out.println("Activating edge " + edge);
        
        Set<Consumer<CriticalEdge>> observers;
        synchronized (this.observers) {
            observers = this.observers.removeAll(edge);
        }
        
        for (Consumer<CriticalEdge> observer : observers) {
            observer.accept(edge);
        }
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
            });
        }
    }
    
    private String getTarget(ITerm term) {
        final String owner;
        if (term instanceof ITermVar) {
            owner = ((ITermVar) term).getResource();
        } else if (term instanceof Scope) {
            owner = ((Scope) term).getResource();
        } else {
            throw new IllegalArgumentException("Expected variable or scope, but was " + term);
        }
        
        return owner;
    }
    
    /**
     * @param term
     *      the term
     * 
     * @return
     *      the completeness to which the request for the given term should be redirected
     */
    private RedirectingIncrementalCompleteness getTargetCompleteness(ITerm term) {
        final String owner = getTarget(term);
        
        if (this.owner.equals(owner)) return this;
        
        return SolverContext.context().getModuleUnchecked(owner).getCurrentState().solver().getCompleteness();
    }
    
    @Override
    public String toString() {
        return "RedirectingIncrementalCompleteness<" + owner + ">";
    }
}
