package mb.statix.solver.completeness;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.collections.MultiSet;
import mb.nabl2.util.collections.MultiSetMap;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public abstract class Completeness implements ICompleteness {

    protected abstract MultiSetMap<ITerm, EdgeOrData<ITerm>> incomplete();

    @Override public boolean isEmpty() {
        // we assume there are no entries with empty values
        return incomplete().isEmpty();
    }

    @Override public MultiSet<EdgeOrData<ITerm>> get(ITerm scopeOrVar, IUniDisunifier unifier) {
        return getVarOrScope(scopeOrVar, unifier).map(sOV -> {
            return incomplete().get(sOV);
        }).orElse(MultiSet.Immutable.of());
    }

    @Override public boolean isComplete(Scope scope, EdgeOrData<ITerm> label, IUniDisunifier unifier) {
        if(!label.match(acc -> true, lbl -> lbl.isGround())) {
            throw new IllegalArgumentException("Label must be ground, got " + label);
        }
        return getVarOrScope(scope, unifier).map(sOV -> {
            return !(incomplete().containsKey(sOV) && incomplete().get(sOV).count(label) > 0);
        }).orElse(true);
    }

    protected static Optional<ITerm> getVarOrScope(ITerm scopeOrVar, IUniDisunifier unifier) {
        return CompletenessUtil.scopeOrVar().match(scopeOrVar, unifier);
    }

    public static class Immutable extends Completeness implements ICompleteness.Immutable, Serializable {

        private static final long serialVersionUID = 1L;

        private final Spec spec;
        private final MultiSetMap.Immutable<ITerm, EdgeOrData<ITerm>> incomplete;

        private Immutable(Spec spec, MultiSetMap.Immutable<ITerm, EdgeOrData<ITerm>> incomplete) {
            this.spec = spec;
            this.incomplete = incomplete;
        }

        @Override protected MultiSetMap<ITerm, EdgeOrData<ITerm>> incomplete() {
            return incomplete;
        }

        @Override public Completeness.Transient melt() {
            return new Completeness.Transient(spec, incomplete.melt());
        }

        public static Completeness.Immutable of(Spec spec) {
            return new Completeness.Immutable(spec, MultiSetMap.Immutable.of());
        }

        @Override public boolean equals(Object o) {
            if(this == o)
                return true;
            if(o == null || getClass() != o.getClass())
                return false;
            Completeness.Immutable immutable = (Completeness.Immutable) o;
            return spec.equals(immutable.spec) && incomplete.equals(immutable.incomplete);
        }

        @Override public int hashCode() {
            return Objects.hash(spec, incomplete);
        }
    }

    public static class Transient extends Completeness implements ICompleteness.Transient {

        private final Spec spec;
        private final MultiSetMap.Transient<ITerm, EdgeOrData<ITerm>> incomplete;

        private Transient(Spec spec, MultiSetMap.Transient<ITerm, EdgeOrData<ITerm>> incomplete) {
            this.spec = spec;
            this.incomplete = incomplete;
        }

        @Override protected MultiSetMap<ITerm, EdgeOrData<ITerm>> incomplete() {
            return incomplete;
        }

        @Override public void add(IConstraint constraint, IUniDisunifier unifier) {
            CompletenessUtil.criticalEdges(constraint, spec, (scopeTerm, label) -> {
                getVarOrScope(scopeTerm, unifier).ifPresent(scopeOrVar -> {
                    incomplete.put(scopeOrVar, label);
                });
            });
        }

        @Override public Set<CriticalEdge> remove(IConstraint constraint, IUniDisunifier unifier) {
            final Set.Transient<CriticalEdge> removedEdges = Set.Transient.of();
            CompletenessUtil.criticalEdges(constraint, spec, (scopeTerm, label) -> {
                getVarOrScope(scopeTerm, unifier).ifPresent(scopeOrVar -> {
                    final int n = incomplete.remove(scopeOrVar, label);
                    if(n == 0) {
                        removedEdges.__insert(CriticalEdge.of(scopeOrVar, label));
                    }
                });
            });
            return removedEdges.freeze();
        }

        @Override public void update(ITermVar var, IUniDisunifier unifier) {
            if(!incomplete.containsKey(var)) {
                return;
            }
            final MultiSet.Immutable<EdgeOrData<ITerm>> updatedLabels = incomplete.removeKey(var);
            getVarOrScope(var, unifier).ifPresent(scopeOrVar -> {
                incomplete.putAll(scopeOrVar, updatedLabels);
            });
        }

        @Override public Completeness.Immutable freeze() {
            return new Completeness.Immutable(spec, incomplete.freeze());
        }

        public static Completeness.Transient of(Spec spec) {
            return new Completeness.Transient(spec, MultiSetMap.Transient.of());
        }

    }

    @Override public String toString() {
        return incomplete().toString();
    }

}
