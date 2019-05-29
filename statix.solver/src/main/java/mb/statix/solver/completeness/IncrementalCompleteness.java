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
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class IncrementalCompleteness implements ICompleteness {

    protected final Spec spec;
    protected final Map<ITerm, Multiset<ITerm>> incomplete;

    public IncrementalCompleteness(Spec spec) {
        this(spec, new HashMap<>());
    }
    
    protected IncrementalCompleteness(Spec spec, Map<ITerm, Multiset<ITerm>> incomplete) {
        this.spec = spec;
        this.incomplete = incomplete;
    }
    
    protected <T> Multiset<T> createMultiset() {
        return HashMultiset.create();
    }

    @Override public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        if(!label.isGround()) {
            throw new IllegalArgumentException("Label must be ground");
        }
        //TODO Taico Small optimization
        Multiset<ITerm> incompleteSet = incomplete.get(scope);
        return incompleteSet == null || !incompleteSet.contains(label); //no incompleteness or not incomplete in the given label
        
//        return !(incomplete.containsKey(scope) && incomplete.get(scope).contains(label));
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
    
    protected void add(ITerm scope, ITerm label) {
        final Multiset<ITerm> labels = incomplete.computeIfAbsent(scope, s -> createMultiset());
        labels.add(label);
    }

    protected void remove(ITerm scope, ITerm label) {
        final Multiset<ITerm> labels = incomplete.computeIfAbsent(scope, s -> createMultiset());
        labels.remove(label);
    }

    @Override public void update(ITermVar var, IUnifier unifier) {
        final Multiset<ITerm> labels = incomplete.remove(var);
        if(labels != null) {
            getVarOrScope(var, unifier).ifPresent(scope -> {
                incomplete.computeIfAbsent(scope, s -> createMultiset()).addAll(labels);
            });
        }
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