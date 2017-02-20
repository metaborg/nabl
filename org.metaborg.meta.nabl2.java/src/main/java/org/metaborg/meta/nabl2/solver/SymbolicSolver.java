package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Set;

import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint.CheckedCases;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Sets;

public class SymbolicSolver extends AbstractSolverComponent<ISymbolicConstraint> {

    private final Set<ITerm> facts;
    private final Set<ITerm> goals;

    public SymbolicSolver() {
        this.facts = Sets.newHashSet();
        this.goals = Sets.newHashSet();
    }

    @Override public Class<ISymbolicConstraint> getConstraintClass() {
        return ISymbolicConstraint.class;
    }

    @Override public Unit add(ISymbolicConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(fact -> {
            facts.add(fact.getFact());
            return unit;
        }, goal -> {
            goals.add(goal.getGoal());
            return unit;
        }));
    }

    @Override public boolean iterate() {
        return false;
    }

    @Override public Iterable<ISymbolicConstraint> finish() {
        return Iterables2.empty();
    }

    public ISymbolicConstraints get() {
        return ImmutableSymbolicConstraints.of(facts, goals);
    }

}