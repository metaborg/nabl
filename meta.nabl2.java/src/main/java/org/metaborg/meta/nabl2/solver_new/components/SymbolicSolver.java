package org.metaborg.meta.nabl2.solver_new.components;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.Set;

public class SymbolicSolver extends ASolver<ISymbolicConstraint, SymbolicSolver.SymbolicResult> {

    private final Set.Transient<ITerm> facts;
    private final Set.Transient<ITerm> goals;

    public SymbolicSolver(SolverCore core) {
        super(core);
        this.facts = Set.Transient.of();
        this.goals = Set.Transient.of();
    }

    @Override public boolean add(ISymbolicConstraint constraint) throws InterruptedException {
        constraint.match(ISymbolicConstraint.Cases.of(fact -> {
            return facts.__insert(fact.getFact());
        }, goal -> {
            return goals.__insert(goal.getGoal());
        }));
        work();
        return true;
    }

    public SymbolicResult finish() {
        return ImmutableSymbolicResult.of(facts.freeze(), goals.freeze());
    }

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class SymbolicResult {

        @Value.Parameter public abstract Set.Immutable<ITerm> facts();

        @Value.Parameter public abstract Set.Immutable<ITerm> goals();

    }

}