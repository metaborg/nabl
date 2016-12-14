package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.collections.Multibag;
import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.sets.CDistinct;
import org.metaborg.meta.nabl2.constraints.sets.CSubsetEq;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint.CheckedCases;
import org.metaborg.meta.nabl2.sets.SetEvaluator;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SetSolver implements ISolverComponent<ISetConstraint> {

    private final IMatcher<Multibag<ITerm,ITerm>> evaluator;
    private final Unifier unifier;

    private final Set<ISetConstraint> defered;

    public SetSolver(IMatcher<Multibag<ITerm,ITerm>> elems, Unifier unifier) {
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

    @Override public Iterable<UnsatisfiableException> finish() {
        return defered.stream().map(c -> {
            return c.getMessageInfo().makeException("Unexpected set constraint.", Iterables2.empty());
        }).collect(Collectors.toList());
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
        Optional<Multibag<ITerm,ITerm>> maybeLeftSet = evaluator.match(left);
        Optional<Multibag<ITerm,ITerm>> maybeRightSet = evaluator.match(right);
        if (!(maybeLeftSet.isPresent() && maybeRightSet.isPresent())) {
            return false;
        }
        Multibag<ITerm,ITerm> leftSet = maybeLeftSet.get();
        Multibag<ITerm,ITerm> rightSet = maybeRightSet.get();
        Multibag<ITerm,ITerm> result = Multibag.create();
        result.putAll(leftSet);
        result.keySet().removeAll(rightSet.keySet());
        if (!result.isEmpty()) {
            throw constraint.getMessageInfo().makeException(left + " not a subset of, or equal to " + right, result
                    .values());
        }
        return true;
    }

    private boolean solve(CDistinct constraint) throws UnsatisfiableException {
        ITerm setTerm = unifier.find(constraint.getSet());
        if (!setTerm.isGround()) {
            return false;
        }
        Optional<Multibag<ITerm,ITerm>> maybeSet = evaluator.match(setTerm);
        if (!(maybeSet.isPresent())) {
            return false;
        }
        Multibag<ITerm,ITerm> set = maybeSet.get();
        List<ITerm> duplicates = Lists.newArrayList();
        for (ITerm key : set.keySet()) {
            Collection<ITerm> values = set.get(key);
            if (values.size() > 1) {
                duplicates.addAll(values);
            }
        }
        if (!duplicates.isEmpty()) {
            throw constraint.getMessageInfo().makeException(setTerm + " elements are not distinct", duplicates);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

}