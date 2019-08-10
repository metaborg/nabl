package mb.statix.modular.solver.completeness;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;

public class ThreadSafeCompleteness implements ICompleteness {
    private final ICompleteness original;
    
    private ThreadSafeCompleteness(ICompleteness original) {
        this.original = original;
    }

    @Override
    public synchronized boolean isComplete(Scope scope, ITerm label, IUnifier unifier) {
        return original.isComplete(scope, label, unifier);
    }

    @Override
    public synchronized void add(IConstraint constraint, IUnifier unifier) {
        original.add(constraint, unifier);
    }

    @Override
    public synchronized void remove(IConstraint constraint, IUnifier unifier) {
        original.remove(constraint, unifier);
    }

    @Override
    public synchronized void update(ITermVar var, IUnifier unifier) {
        original.update(var, unifier);
    }
    
    @Override
    public synchronized void addAll(Iterable<? extends IConstraint> constraints, IUnifier unifier) {
        for (IConstraint constraint : constraints) {
            original.add(constraint, unifier);
        }
    }
    
    @Override
    public synchronized void removeAll(Iterable<? extends IConstraint> constraints, IUnifier unifier) {
        for (IConstraint constraint : constraints) {
            original.remove(constraint, unifier);
        }
    }
    
    @Override
    public synchronized void updateAll(Iterable<? extends ITermVar> vars, IUnifier unifier) {
        for (ITermVar var : vars) {
            original.update(var, unifier);
        }
    }
}
