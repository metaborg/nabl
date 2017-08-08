package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.sym.ImmutableCFact;
import org.metaborg.meta.nabl2.constraints.sym.ImmutableCGoal;
import org.metaborg.meta.nabl2.solver.ISymbolicConstraints;
import org.metaborg.meta.nabl2.solver.ImmutableSymbolicConstraints;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Sets;

public class SymbolicSolver extends SolverComponent<ISymbolicConstraint> {

    private final Set<ITerm> facts;
    private final Set<ITerm> goals;

    public SymbolicSolver(Solver solver) {
        super(solver);
        this.facts = Sets.newHashSet();
        this.goals = Sets.newHashSet();
    }

    @Override protected Unit doAdd(ISymbolicConstraint constraint) throws UnsatisfiableException {
        constraint.<Unit, UnsatisfiableException>matchOrThrow(CheckedCases.of(fact -> {
            facts.add(fact.getFact());
            return unit;
        }, goal -> {
            goals.add(goal.getGoal());
            return unit;
        }));
        work();
        return unit;
    }

    @Override protected Set<? extends ISymbolicConstraint> doFinish(IMessageInfo messageInfo)
            throws InterruptedException {
        Set<ISymbolicConstraint> constraints = Sets.newHashSet();
        if(isPartial()) {
            facts.stream().forEach(fact -> constraints.add(ImmutableCFact.of(fact, messageInfo)));
            goals.stream().forEach(goal -> constraints.add(ImmutableCGoal.of(goal, messageInfo)));
        }
        return constraints;
    }

    public ISymbolicConstraints get() {
        return ImmutableSymbolicConstraints.of(facts, goals);
    }

}