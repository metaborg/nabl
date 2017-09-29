package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.base.CConj;
import org.metaborg.meta.nabl2.constraints.base.CExists;
import org.metaborg.meta.nabl2.constraints.base.CNew;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableScope;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.ISubstitution;
import org.metaborg.meta.nabl2.unification.Substitution;

public class BaseComponent extends ASolver {

    public BaseComponent(SolverCore core) {
        super(core);
    }

    public Optional<SolveResult> solve(IBaseConstraint constraint) throws InterruptedException {
        final SolveResult result = constraint.match(IBaseConstraint.Cases.of(
        // @formatter:off
            t -> SolveResult.empty(),
            f -> SolveResult.messages(
                    constraint.getMessageInfo().withDefaultContent(MessageContent.of("False can never be satisfied."))),
            this::solve,
            this::solve,
            this::solve
            // @formatter:on
        ));
        return Optional.of(result);
    }

    private SolveResult solve(CConj constraint) {
        return SolveResult.constraints(constraint.getConstraints());
    }

    private SolveResult solve(CExists constraint) {
        ISubstitution.Transient tsubst = Substitution.Transient.of();
        constraint.getEVars().forEach(var -> tsubst.put(var, TB.newVar(var.getResource(), fresh(var.getName()))));
        ISubstitution.Immutable subst = tsubst.freeze();
        return SolveResult.constraints(Constraints.substitute(constraint.getConstraint(), subst));
    }

    private SolveResult solve(CNew constraint) {
        ISubstitution.Transient tsubst = Substitution.Transient.of();
        constraint.getNVars().forEach(var -> tsubst.put(var, ImmutableScope.of(var.getResource(), fresh(var.getName()))));
        ISubstitution.Immutable subst = tsubst.freeze();
        return SolveResult.constraints(Constraints.substitute(constraint.getConstraint(), subst));
    }

}