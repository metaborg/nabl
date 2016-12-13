package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.sets.CDistinct;
import org.metaborg.meta.nabl2.constraints.sets.CSubsetEq;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint.CheckedCases;
import org.metaborg.meta.nabl2.sets.SetEvaluator;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.unification.Unifier;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class SetSolver implements ISolverComponent<ISetConstraint> {

    private final IMatcher<Multimap<ITerm,ITerm>> evaluator;
    private final Unifier unifier;

    private final Set<ISetConstraint> defered;

    public SetSolver(IMatcher<Multimap<ITerm,ITerm>> elems, Unifier unifier) {
        this.evaluator = SetEvaluator.matcher(elems);
        this.unifier = unifier;
        this.defered = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(ISetConstraint constraint) throws UnsatisfiableException {
        if (!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        Iterator<ISetConstraint> it = defered.iterator();
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
            throw new UnsatisfiableException("Unexpected set constraint.", defered.toArray(new IConstraint[0]));
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(ISetConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(CSubsetEq constraint) throws UnsatisfiableException {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        if (!left.isGround() && right.isGround()) {
            return false;
        }
        Optional<Multimap<ITerm,ITerm>> maybeLeftSet = evaluator.match(left);
        Optional<Multimap<ITerm,ITerm>> maybeRightSet = evaluator.match(right);
        if (!(maybeLeftSet.isPresent() && maybeRightSet.isPresent())) {
            return false;
        }
        Multimap<ITerm,ITerm> leftSet = maybeLeftSet.get();
        Multimap<ITerm,ITerm> rightSet = maybeRightSet.get();
        Multimap<ITerm,ITerm> result = HashMultimap.create(leftSet);
        result.keySet().removeAll(rightSet.keySet());
        if (!result.isEmpty()) {
            throw new UnsatisfiableException(left + " not a subset of, or equal to " + right, constraint);
        }
        return true;
    }

    private boolean solve(CDistinct constraint) throws UnsatisfiableException {
        ITerm setTerm = unifier.find(constraint.getSet());
        if (!setTerm.isGround()) {
            return false;
        }
        Optional<Multimap<ITerm,ITerm>> maybeSet = evaluator.match(setTerm);
        if (!(maybeSet.isPresent())) {
            return false;
        }
        Multimap<ITerm,ITerm> set = maybeSet.get();
        for (ITerm key : set.keySet()) {
            if (set.get(key).size() > 1) {
                throw new UnsatisfiableException(setTerm + " elements are not distinct", constraint);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

}