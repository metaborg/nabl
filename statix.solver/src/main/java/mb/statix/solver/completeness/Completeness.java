package mb.statix.solver.completeness;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;
import java.util.stream.Collectors;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.MultiSet;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public abstract class Completeness implements ICompleteness {

    protected abstract Map<ITerm, ? extends MultiSet<ITerm>> incomplete();

    @Override public boolean isEmpty() {
        // we assume there are no entries with empty values
        return incomplete().isEmpty();
    }

    @Override public boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        if(!label.isGround()) {
            throw new IllegalArgumentException("Label must be ground");
        }
        return getVarOrScope(scope, unifier).map(
                scopeOrVar -> !(incomplete().containsKey(scopeOrVar) && incomplete().get(scopeOrVar).count(label) > 0))
                .orElse(true);
    }

    protected static Optional<ITerm> getVarOrScope(ITerm scope, IUnifier unifier) {
        // @formatter:off
        return M.cases(
            Scope.matcher(),
            M.var()
        ).match(scope, unifier);
        // @formatter:on
    }

    public static class Immutable extends Completeness implements ICompleteness.Immutable {

        private final Spec spec;
        private final Map.Immutable<ITerm, MultiSet.Immutable<ITerm>> incomplete;

        private Immutable(Spec spec, Map.Immutable<ITerm, MultiSet.Immutable<ITerm>> incomplete) {
            this.spec = spec;
            this.incomplete = incomplete;
        }

        @Override protected Map<ITerm, ? extends MultiSet<ITerm>> incomplete() {
            return incomplete;
        }

        @Override public Completeness.Transient melt() {
            return new Completeness.Transient(spec, incomplete.asTransient());
        }

        public static Completeness.Immutable of(Spec spec) {
            return new Completeness.Immutable(spec, Map.Immutable.of());
        }

    }

    public static class Transient extends Completeness implements ICompleteness.Transient {

        private final Spec spec;
        private final Map.Transient<ITerm, MultiSet.Immutable<ITerm>> incomplete;

        private Transient(Spec spec, Map.Transient<ITerm, MultiSet.Immutable<ITerm>> incomplete) {
            this.spec = spec;
            this.incomplete = incomplete;
        }

        @Override protected Map<ITerm, ? extends MultiSet<ITerm>> incomplete() {
            return incomplete;
        }

        @Override public void add(IConstraint constraint, IUnifier unifier) {
            CompletenessUtil.criticalEdges(constraint, spec, (scopeTerm, label) -> {
                getVarOrScope(scopeTerm, unifier).ifPresent(scopeOrVar -> {
                    final MultiSet.Transient<ITerm> labels =
                            incomplete.getOrDefault(scopeOrVar, MultiSet.Immutable.of()).melt();
                    labels.add(label);
                    incomplete.__put(scopeOrVar, labels.freeze());
                });
            });
        }

        @Override public Set<CriticalEdge> remove(IConstraint constraint, IUnifier unifier) {
            final Set.Transient<CriticalEdge> removedEdges = Set.Transient.of();
            CompletenessUtil.criticalEdges(constraint, spec, (scopeTerm, label) -> {
                getVarOrScope(scopeTerm, unifier).ifPresent(scopeOrVar -> {
                    final MultiSet.Transient<ITerm> labels =
                            incomplete.getOrDefault(scopeOrVar, MultiSet.Immutable.of()).melt();
                    if(labels.remove(label) == 0) {
                        removedEdges.__insert(CriticalEdge.of(scopeOrVar, label));
                    }
                    if(labels.isEmpty()) {
                        incomplete.__remove(scopeOrVar);
                    } else {
                        incomplete.__put(scopeOrVar, labels.freeze());
                    }
                });
            });
            return removedEdges.freeze();
        }

        @Override public void update(ITermVar var, IUnifier unifier) {
            final MultiSet<ITerm> updatedLabels = incomplete.__remove(var);
            if(updatedLabels != null) {
                getVarOrScope(var, unifier).ifPresent(scopeOrVar -> {
                    final MultiSet.Transient<ITerm> labels =
                            incomplete.getOrDefault(scopeOrVar, MultiSet.Immutable.of()).melt();
                    updatedLabels.forEach(labels::add);
                    incomplete.__put(scopeOrVar, labels.freeze());
                });
            }
        }

        @Override public Completeness.Immutable freeze() {
            return new Completeness.Immutable(spec, incomplete.freeze());
        }

        public static Completeness.Transient of(Spec spec) {
            return new Completeness.Transient(spec, Map.Transient.of());
        }

    }

    @Override public String toString() {
        return incomplete().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

}