package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.sym.ImmutableCFact;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SymbolicSolver extends AbstractSolverComponent<ISymbolicConstraint> {

    private final Unifier unifier;
    private final Set<ITerm> facts;
    private final Set<ITerm> goals;

    public SymbolicSolver(Unifier unifier) {
        this.unifier = unifier;
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

    @Override public Collection<ISymbolicConstraint> getNormalizedConstraints(IMessageInfo messageInfo) {
        List<ISymbolicConstraint> constraints = Lists.newArrayList();
        facts.stream().map(unifier::find).forEach(fact -> constraints.add(ImmutableCFact.of(fact, messageInfo)));
        goals.stream().map(unifier::find).forEach(goal -> constraints.add(ImmutableCFact.of(goal, messageInfo)));
        return constraints;
    }

    public ISymbolicConstraints get() {
        return ImmutableSymbolicConstraints.of(facts, goals);
    }

}