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

    private final Spec spec;
    private final Map<ITerm, Multiset<ITerm>> incomplete;

    public IncrementalCompleteness(Spec spec) {
        this.spec = spec;
        this.incomplete = new HashMap<>();
    }

    @Override public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        if(!label.isGround()) {
            throw new IllegalArgumentException("Label must be ground");
        }
        return !(incomplete.containsKey(scope) && incomplete.get(scope).contains(label));
    }

    @Override public void add(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec).stream().forEach(c -> {
            getVarOrScope(c.scope(), unifier).ifPresent(scope -> {
                final ITerm label = c.label();
                final Multiset<ITerm> labels = incomplete.computeIfAbsent(scope, s -> HashMultiset.create());
                labels.add(label);
            });
        });
    }

    @Override public void remove(IConstraint constraint, IUnifier unifier) {
        Completeness.criticalEdges(constraint, spec).stream().forEach(c -> {
            getVarOrScope(c.scope(), unifier).ifPresent(scope -> {
                final ITerm label = c.label();
                final Multiset<ITerm> labels = incomplete.computeIfAbsent(scope, s -> HashMultiset.create());
                labels.remove(label);
            });
        });
    }

    @Override public void update(ITermVar var, IUnifier unifier) {
        final Multiset<ITerm> labels = incomplete.remove(var);
        if(labels != null) {
            getVarOrScope(var, unifier).ifPresent(scope -> {
                incomplete.computeIfAbsent(scope, s -> HashMultiset.create()).addAll(labels);
            });
        }
    }

    private Optional<ITerm> getVarOrScope(ITerm scope, IUnifier unifier) {
        // @formatter:off
        return M.cases(
            Scope.matcher(),
            M.var()
        ).match(scope, unifier);
        // @formatter:on
    }

}