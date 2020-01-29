package mb.statix.solver.completeness;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.modular.observers.EdgeCompleteObserver;
import mb.statix.modular.util.TDebug;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class IncrementalCompleteness implements ICompleteness {

    protected final Spec spec;
    protected final Map<ITerm, Multiset<ITerm>> incomplete;
    protected EdgeCompleteObserver edgeCompleteObserver;

    public IncrementalCompleteness(Spec spec) {
        this(spec, new HashMap<>());
    }
    
    protected IncrementalCompleteness(Spec spec, Map<ITerm, Multiset<ITerm>> incomplete) {
        this.spec = spec;
        this.incomplete = incomplete;
    }
    
    /**
     * Creates a new multiset. Overriding classes can override this method if they want a
     * different type of multisets.
     * 
     * @return
     *      a new multiset
     */
    protected <T> Multiset<T> createMultiset() {
        return HashMultiset.create();
    }

    @Override public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        return _isComplete(scope, label);
    }
    
    protected boolean _isComplete(Scope scope, ITerm label) {
        if(!label.isGround()) {
            throw new IllegalArgumentException("Label must be ground");
        }
        return !(incomplete.containsKey(scope) && incomplete.get(scope).contains(label));
    }

    @Override public void add(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> add(scope, label));
        });
    }

    @Override public void remove(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec, (scopeTerm, label) -> {
            getVarOrScope(scopeTerm, unifier).ifPresent(scope -> remove(scope, label));
        });
    }
    
    @Override public void update(ITermVar var, IUnifier unifier) {
        final Multiset<ITerm> labels = incomplete.remove(var);
        if(labels != null) {
            getVarOrScope(var, unifier).ifPresent(scope -> {
                incomplete.computeIfAbsent(scope, s -> createMultiset()).addAll(labels);
            });
        }
    }
    
    /**
     * Adds the given edge as incomplete.
     * 
     * @param scope
     *      the scope of the edge
     * @param label
     *      the label of the edge
     */
    protected void add(ITerm scope, ITerm label) {
        if (TDebug.COMPLETENESS) TDebug.DEV_OUT.info("Adding " + scope + "-" + label + " as incomplete in " + this);
        final Multiset<ITerm> labels = incomplete.computeIfAbsent(scope, s -> createMultiset());
        labels.add(label);
    }

    /**
     * Removes the given edge from incomplete edges.
     * 
     * @param scope
     *      the scope of the edge
     * @param label
     *      the label of the edge
     */
    protected void remove(ITerm scope, ITerm label) {
        if (TDebug.COMPLETENESS) TDebug.DEV_OUT.info("Removing " + scope + "-" + label + " from incomplete in " + this);
        final Multiset<ITerm> labels = incomplete.computeIfAbsent(scope, s -> createMultiset());
        //Remove one label from the set. If the multiset now no longer contains the label, trigger the observer.
        if (labels.remove(label) && edgeCompleteObserver != null && !labels.contains(label)) {
            edgeCompleteObserver.accept(scope, label);
        }
    }
    
    /**
     * Sets the observer for edge completeness.
     * 
     * @param observer
     *      the observer for edge completeness
     */
    public void setEdgeCompleteObserver(EdgeCompleteObserver observer) {
        this.edgeCompleteObserver = observer;
    }

    protected Optional<ITerm> getVarOrScope(ITerm scope, IUnifier unifier) {
        // @formatter:off
        return M.cases(
            Scope.matcher(),
            M.var()
        ).match(scope, unifier);
        // @formatter:on
    }

}