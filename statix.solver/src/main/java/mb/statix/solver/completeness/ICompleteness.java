package mb.statix.solver.completeness;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;

public interface ICompleteness {

    boolean isComplete(Scope scope, ITerm label, IUnifier unifier);

    void add(IConstraint constraint, IUnifier unifier);

    default void addAll(Iterable<? extends IConstraint> constraints, IUnifier unifier) {
        for (IConstraint constraint : constraints) {
            add(constraint, unifier);
        }
    }

    void remove(IConstraint constraint, IUnifier unifier);

    default void removeAll(Iterable<? extends IConstraint> constraints, IUnifier unifier) {
        for (IConstraint constraint : constraints) {
            remove(constraint, unifier);
        }
    }

    void update(ITermVar var, IUnifier unifier);

    default void updateAll(Iterable<? extends ITermVar> vars, IUnifier unifier) {
        for (ITermVar var : vars) {
            update(var, unifier);
        }
    }

}