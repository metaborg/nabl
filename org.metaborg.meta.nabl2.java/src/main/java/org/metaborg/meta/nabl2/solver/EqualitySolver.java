package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.Iterator;
import java.util.Set;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.equality.CEqual;
import org.metaborg.meta.nabl2.constraints.equality.CInequal;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint.CheckedCases;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;

import com.google.common.collect.Sets;

public class EqualitySolver implements ISolverComponent<IEqualityConstraint> {

    private final Unifier unifier;

    private final Set<IEqualityConstraint> defered;

    public EqualitySolver(Unifier unifier) {
        this.unifier = unifier;
        this.defered = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(IEqualityConstraint constraint) throws UnsatisfiableException {
        if (!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        Iterator<IEqualityConstraint> it = defered.iterator();
        boolean progress = false;
        while (it.hasNext()) {
            try {
                if (solve(it.next())) {
                    progress = true;
                    it.remove();
                }
            } catch (UnsatisfiableException e) {
                progress = true;
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    @Override public void finish() throws UnsatisfiableException {
        if (!defered.isEmpty()) {
            throw new UnsatisfiableException("Unexpected unsolved equality.", defered.toArray(new IConstraint[0]));
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IEqualityConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(CEqual constraint) throws UnsatisfiableException {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        try {
            unifier.unify(left, right);
        } catch (UnificationException ex) {
            throw new UnsatisfiableException("Cannot unify " + left + " with " + right, ex, constraint);
        }
        return true;
    }

    private boolean solve(CInequal constraint) throws UnsatisfiableException {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        if (left.equals(right)) {
            throw new UnsatisfiableException("Terms are not inequal.", constraint);
        }
        return !unifier.canUnify(left, right);
    }

    // ------------------------------------------------------------------------------------------------------//

}