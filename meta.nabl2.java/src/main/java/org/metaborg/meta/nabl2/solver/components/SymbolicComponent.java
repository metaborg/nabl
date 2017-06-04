package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.symbolic.ImmutableSymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.Set;

public class SymbolicComponent extends ASolver<ISymbolicConstraint, ISymbolicConstraints> {

    private final Set.Transient<ITerm> facts;
    private final Set.Transient<ITerm> goals;

    public SymbolicComponent(SolverCore core, ISymbolicConstraints initial) {
        super(core);
        this.facts = initial.getFacts().asTransient();
        this.goals = initial.getGoals().asTransient();
    }

    @Override public SeedResult seed(ISymbolicConstraints solution, IMessageInfo message) throws InterruptedException {
        facts.__insertAll(solution.getFacts());
        goals.__insertAll(solution.getGoals());
        return SeedResult.empty();
    }

    @Override public Optional<SolveResult> solve(ISymbolicConstraint constraint) throws InterruptedException {
        constraint.match(ISymbolicConstraint.Cases.of(
            // @formatter:off
            fact -> facts.__insert(fact.getFact()),
            goal -> goals.__insert(goal.getGoal())
            // @formatter:on
        ));
        return Optional.of(SolveResult.empty());
    }

    public ISymbolicConstraints finish() {
        return ImmutableSymbolicConstraints.of(facts.freeze(), goals.freeze());
    }

}