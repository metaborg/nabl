package mb.statix.solver.completeness;

import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;

public interface ICompleteness {

    boolean isEmpty();
    
    boolean isComplete(Scope scope, ITerm label, IUniDisunifier unifier);

    interface Immutable extends ICompleteness {

        ICompleteness.Transient melt();

    }

    interface Transient extends ICompleteness {

        void add(IConstraint constraint, IUniDisunifier unifier);

        default void addAll(Iterable<? extends IConstraint> constraints, IUniDisunifier unifier) {
            Iterables2.stream(constraints).forEach(c -> add(c, unifier));
        }

        Set<CriticalEdge> remove(IConstraint constraint, IUniDisunifier unifier);

        default Set<CriticalEdge> removeAll(Iterable<? extends IConstraint> constraints, IUniDisunifier unifier) {
            return Iterables2.stream(constraints).flatMap(c -> remove(c, unifier).stream())
                    .collect(ImmutableSet.toImmutableSet());
        }

        void update(ITermVar var, IUniDisunifier unifier);

        default void updateAll(Iterable<? extends ITermVar> vars, IUniDisunifier unifier) {
            Iterables2.stream(vars).forEach(c -> update(c, unifier));
        }

        ICompleteness.Immutable freeze();

    }

}