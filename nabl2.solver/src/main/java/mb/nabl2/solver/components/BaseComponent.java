package mb.nabl2.solver.components;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.base.CConj;
import mb.nabl2.constraints.base.CExists;
import mb.nabl2.constraints.base.CNew;
import mb.nabl2.constraints.base.IBaseConstraint;
import mb.nabl2.constraints.equality.ImmutableCEqual;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.scopegraph.terms.ImmutableScope;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.UnificationException;

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
        final IUnifier.Transient tsubst = PersistentUnifier.Transient.of();
        constraint.getEVars().forEach(var -> {
            try {
                tsubst.unify(var, B.newVar(var.getResource(), fresh(var.getName())));
            } catch(UnificationException e) {
                throw new IllegalArgumentException("Evars should be distinct.");
            }
        });
        final IUnifier.Immutable subst = tsubst.freeze();
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
        return M.var(v -> ImmutableScope.of(v.getResource(), fresh(v.getName()))).match(term, unifier())
                .orElseGet(() -> ImmutableScope.of("", fresh("s")));
    }

}