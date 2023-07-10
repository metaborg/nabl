package mb.statix.solver.completeness;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.stream.StreamUtil;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.CriticalEdge;

public abstract class Completeness implements ICompleteness {

    private static final Immutable EMPTY = new Immutable(MultiSetMap.Immutable.of());

    protected abstract MultiSetMap<ITerm, EdgeOrData<ITerm>> asMap();

    @Override public java.util.Set<Entry<ITerm, MultiSet.Immutable<EdgeOrData<ITerm>>>> entrySet() {
        return asMap().entrySet();
    }

    @Override public boolean isEmpty() {
        // we assume there are no entries with empty values
        return asMap().isEmpty();
    }

    @Override public MultiSet.Immutable<EdgeOrData<ITerm>> get(ITerm scopeOrVar, IUnifier unifier) {
        return getVarOrScope(scopeOrVar, unifier).map(sOV -> {
            return asMap().get(sOV);
        }).orElse(MultiSet.Immutable.of());
    }

    @Override public boolean isComplete(Scope scope, EdgeOrData<ITerm> label, IUnifier unifier) {
        if(!label.match(() -> true, lbl -> lbl.isGround())) {
            throw new IllegalArgumentException("Label must be ground, got " + label);
        }
        return getVarOrScope(scope, unifier).map(sOV -> {
            return !(asMap().containsKey(sOV) && asMap().get(sOV).count(label) > 0);
        }).orElse(true);
    }

    protected static java.util.Set<ITerm> getVarsOrScopes(Collection<? extends ITerm> varOrScopes, IUnifier unifier) {
        return StreamUtil.filterMap(varOrScopes.stream(), t -> getVarOrScope(t, unifier))
                .collect(Collectors.toSet());
    }

    protected static Optional<ITerm> getVarOrScope(ITerm scopeOrVar, IUnifier unifier) {
        return CompletenessUtil.scopeOrVar().match(scopeOrVar, unifier);
    }

    public static class Immutable extends Completeness implements ICompleteness.Immutable, Serializable {

        private static final long serialVersionUID = 1L;

        private final MultiSetMap.Immutable<ITerm, EdgeOrData<ITerm>> incomplete;

        private Immutable(MultiSetMap.Immutable<ITerm, EdgeOrData<ITerm>> incomplete) {
            this.incomplete = incomplete;
        }

        @Override protected MultiSetMap.Immutable<ITerm, EdgeOrData<ITerm>> asMap() {
            return incomplete;
        }

        @Override public mb.statix.solver.completeness.ICompleteness.Immutable
                addAll(ICompleteness.Immutable criticalEdges, IUnifier unifier) {
            final Completeness.Transient _completeness = melt();
            _completeness.addAll(criticalEdges, unifier);
            return _completeness.freeze();
        }

        @Override public ICompleteness.Immutable apply(ISubstitution.Immutable subst) {
            final Completeness.Transient _completeness = melt();
            _completeness.apply(subst);
            return _completeness.freeze();
        }

        @Override public ICompleteness.Immutable apply(IRenaming subst) {
            final Completeness.Transient _completeness = melt();
            _completeness.apply(subst);
            return _completeness.freeze();
        }

        @Override public ICompleteness.Immutable removeAll(Collection<? extends ITerm> varOrScopes, IUnifier unifier) {
            return new Completeness.Immutable(incomplete.removeAll(getVarsOrScopes(varOrScopes, unifier)));
        }

        @Override public ICompleteness.Immutable retainAll(Collection<? extends ITerm> varOrScopes, IUnifier unifier) {
            return new Completeness.Immutable(incomplete.retainAll(getVarsOrScopes(varOrScopes, unifier)));
        }

        @Override public ICompleteness.Immutable updateAll(Iterable<? extends ITermVar> vars, IUnifier unifier) {
            final Completeness.Transient _completeness = melt();
            _completeness.updateAll(vars, unifier);
            return _completeness.freeze();
        }

        @Override public Completeness.Transient melt() {
            return new Completeness.Transient(incomplete.melt());
        }

        public static Completeness.Immutable of() {
            return EMPTY;
        }

        @Override public boolean equals(Object o) {
            if(this == o)
                return true;
            if(o == null || getClass() != o.getClass())
                return false;
            Completeness.Immutable immutable = (Completeness.Immutable) o;
            return incomplete.equals(immutable.incomplete);
        }

        @Override public int hashCode() {
            return incomplete.hashCode();
        }

    }

    public static class Transient extends Completeness implements ICompleteness.Transient {

        private final MultiSetMap.Transient<ITerm, EdgeOrData<ITerm>> incomplete;

        private Transient(MultiSetMap.Transient<ITerm, EdgeOrData<ITerm>> incomplete) {
            this.incomplete = incomplete;
        }

        @Override protected MultiSetMap<ITerm, EdgeOrData<ITerm>> asMap() {
            return incomplete;
        }

        @Override public void add(ITerm scopeTerm, EdgeOrData<ITerm> label, IUnifier unifier) {
            getVarOrScope(scopeTerm, unifier).ifPresent(scopeOrVar -> {
                incomplete.put(scopeOrVar, label);
            });
        }

        @Override public void addAll(ICompleteness.Immutable criticalEdges, IUnifier unifier) {
            for(Entry<ITerm, MultiSet.Immutable<EdgeOrData<ITerm>>> varLabel : criticalEdges.entrySet()) {
                getVarOrScope(varLabel.getKey(), unifier).ifPresent(scopeOrVar -> {
                    incomplete.putAll(scopeOrVar, varLabel.getValue());
                });
            }
        }

        @Override public Set.Immutable<CriticalEdge> remove(ITerm scopeTerm, EdgeOrData<ITerm> label,
                IUnifier unifier) {
            final Set.Transient<CriticalEdge> removedEdges = CapsuleUtil.transientSet();
            getVarOrScope(scopeTerm, unifier).ifPresent(scopeOrVar -> {
                final int n = incomplete.remove(scopeOrVar, label);
                if(n == 0) {
                    throw new IllegalStateException("Absent critical edge: " + scopeOrVar + "/" + label);
                }
                if(n == 1) {
                    removedEdges.__insert(CriticalEdge.of(scopeOrVar, label));
                }
            });
            return removedEdges.freeze();
        }

        @Override public Set.Immutable<CriticalEdge> removeAll(ICompleteness.Immutable criticalEdges,
                IUnifier unifier) {
            final Set.Transient<CriticalEdge> removedEdges = CapsuleUtil.transientSet();
            for(Entry<ITerm, MultiSet.Immutable<EdgeOrData<ITerm>>> varLabel : criticalEdges.entrySet()) {
                getVarOrScope(varLabel.getKey(), unifier).ifPresent(scopeOrVar -> {
                    for(Entry<EdgeOrData<ITerm>, Integer> labelCount : varLabel.getValue().entrySet()) {
                        final int n = incomplete.remove(scopeOrVar, labelCount.getKey(), labelCount.getValue());
                        if(n == 0) {
                            throw new IllegalStateException("Absent critical edge: " + scopeOrVar + "/"
                                    + labelCount.getKey() + "#" + labelCount.getValue());
                        }
                        if(n == 1) {
                            removedEdges.__insert(CriticalEdge.of(scopeOrVar, labelCount.getKey()));
                        }
                    }
                });
            }
            return removedEdges.freeze();
        }

        @Override public void update(ITermVar var, IUnifier unifier) {
            if(!incomplete.containsKey(var)) {
                return;
            }
            final MultiSet.Immutable<EdgeOrData<ITerm>> updatedLabels = incomplete.removeKey(var);
            getVarOrScope(var, unifier).ifPresent(scopeOrVar -> {
                incomplete.putAll(scopeOrVar, updatedLabels);
            });
        }

        @Override public void apply(ISubstitution.Immutable subst) {
            final MultiSetMap.Transient<ITerm, EdgeOrData<ITerm>> newEntries = MultiSetMap.Transient.of();
            for(Entry<ITermVar, ITerm> varTerm : subst.entrySet()) {
                final MultiSet.Immutable<EdgeOrData<ITerm>> updatedLabels = incomplete.removeKey(varTerm.getKey());
                if(!updatedLabels.isEmpty()) {
                    getVarOrScope(varTerm.getValue(), PersistentUnifier.Immutable.of()).ifPresent(scopeOrVar -> {
                        newEntries.putAll(scopeOrVar, updatedLabels);
                    });
                }
            }
            for(Entry<ITerm, MultiSet.Immutable<EdgeOrData<ITerm>>> varTerm : newEntries.asMap().entrySet()) {
                incomplete.putAll(varTerm.getKey(), varTerm.getValue());
            }
        }

        @Override public void apply(IRenaming renaming) {
            final MultiSetMap.Transient<ITermVar, EdgeOrData<ITerm>> newEntries = MultiSetMap.Transient.of();
            for(ITermVar var : renaming.keySet()) {
                final MultiSet.Immutable<EdgeOrData<ITerm>> updatedLabels = incomplete.removeKey(var);
                if(!updatedLabels.isEmpty()) {
                    newEntries.putAll(renaming.rename(var), updatedLabels);
                }
            }
            for(Entry<ITermVar, MultiSet.Immutable<EdgeOrData<ITerm>>> varTerm : newEntries.asMap().entrySet()) {
                incomplete.putAll(varTerm.getKey(), varTerm.getValue());
            }
        }

        @Override public Completeness.Immutable freeze() {
            return incomplete.isEmpty() ? EMPTY : new Completeness.Immutable(incomplete.freeze());
        }

        public static Completeness.Transient of() {
            return new Completeness.Transient(MultiSetMap.Transient.of());
        }

    }

    @Override public String toString() {
        return asMap().toString();
    }

}
