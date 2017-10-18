package org.metaborg.meta.nabl2.solver.components;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.base.CConj;
import org.metaborg.meta.nabl2.constraints.base.CExists;
import org.metaborg.meta.nabl2.constraints.base.CNew;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableScope;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.ISubstitution;
import org.metaborg.meta.nabl2.unification.Substitution;

import com.google.common.collect.Lists;

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
        return SolveResult.constraints(constraint.getLeft(), constraint.getRight());
    }

    private SolveResult solve(CExists constraint) {
        ISubstitution.Transient tsubst = Substitution.Transient.of();
        constraint.getEVars().forEach(var -> tsubst.put(var, TB.newVar(var.getResource(), fresh(var.getName()))));
        ISubstitution.Immutable subst = tsubst.freeze();
        return SolveResult.constraints(Constraints.substitute(constraint.getConstraint(), subst));
    }

    private SolveResult solve(CNew constraint) {
        final List<IConstraint> constraints = Lists.newArrayList();
        for(ITerm scope : constraint.getNVars()) {
            constraints.add(ImmutableCEqual.of(scope, newScope(scope), constraint.getMessageInfo()));
        }
        return SolveResult.constraints(constraints);
    }

    private Scope newScope(ITerm term) {
        return M.var(v -> ImmutableScope.of(v.getResource(), fresh(v.getName()))).match(term)
                .orElseGet(() -> ImmutableScope.of("", fresh("s")));
    }

}