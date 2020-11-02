package mb.statix.solver.completeness;

import java.util.Map.Entry;

import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.collections.MultiSet;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public interface ICompleteness {

    boolean isEmpty();

    MultiSet<EdgeOrData<ITerm>> get(ITerm varOrScope, IUniDisunifier unifier);

    boolean isComplete(Scope scope, EdgeOrData<ITerm> label, IUniDisunifier unifier);

    java.util.Set<Entry<ITerm, MultiSet.Immutable<EdgeOrData<ITerm>>>> entrySet();

    interface Immutable extends ICompleteness {

        Immutable apply(ISubstitution.Immutable subst);

        Immutable apply(IRenaming renaming);

        ICompleteness.Transient melt();

    }

    interface Transient extends ICompleteness {

        void add(ITerm varOrScope, EdgeOrData<ITerm> label, IUniDisunifier unifier);

        default void add(IConstraint constraint, Spec spec, IUniDisunifier unifier) {
            CompletenessUtil.criticalEdges(constraint, spec, (scopeTerm, label) -> {
                add(scopeTerm, label, unifier);
            });
        }

        default void addAll(Iterable<? extends IConstraint> constraints, Spec spec, IUniDisunifier unifier) {
            Iterables2.stream(constraints).forEach(c -> add(c, spec, unifier));
        }

        void addAll(ICompleteness.Immutable criticalEdges, IUniDisunifier unifier);


        Set.Immutable<CriticalEdge> remove(ITerm varOrScope, EdgeOrData<ITerm> label, IUniDisunifier unifier);

        default Set.Immutable<CriticalEdge> remove(IConstraint constraint, Spec spec, IUniDisunifier unifier) {
            final Set.Transient<CriticalEdge> removedEdges = Set.Transient.of();
            CompletenessUtil.criticalEdges(constraint, spec, (scopeTerm, label) -> {
                removedEdges.__insertAll(remove(scopeTerm, label, unifier));
            });
            return removedEdges.freeze();
        }

        default Set.Immutable<CriticalEdge> removeAll(Iterable<? extends IConstraint> constraints, Spec spec,
                IUniDisunifier unifier) {
            return Iterables2.stream(constraints).flatMap(c -> remove(c, spec, unifier).stream())
                    .collect(CapsuleCollectors.toSet());
        }

        Set.Immutable<CriticalEdge> removeAll(ICompleteness.Immutable criticalEdges, IUniDisunifier unifier);


        void update(ITermVar var, IUniDisunifier unifier);

        default void updateAll(Iterable<? extends ITermVar> vars, IUniDisunifier unifier) {
            Iterables2.stream(vars).forEach(c -> update(c, unifier));
        }


        void apply(ISubstitution.Immutable subst);

        void apply(IRenaming renaming);


        ICompleteness.Immutable freeze();

    }

}