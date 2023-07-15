package mb.nabl2.solver.components;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.List;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.base.CConj;
import mb.nabl2.constraints.base.CExists;
import mb.nabl2.constraints.base.CNew;
import mb.nabl2.constraints.base.IBaseConstraint;
import mb.nabl2.constraints.equality.CEqual;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.scopegraph.pepm16.terms.Scope;

public class BaseComponent extends ASolver {

    public BaseComponent(SolverCore core) {
        super(core);
    }

    public SolveResult solve(IBaseConstraint constraint) {
        return constraint.match(IBaseConstraint.Cases.of(
        // @formatter:off
            t -> SolveResult.empty(),
            f -> SolveResult.messages(
                    constraint.getMessageInfo().withDefaultContent(MessageContent.of("False can never be satisfied."))),
            this::solve,
            this::solve,
            this::solve
            // @formatter:on
        ));
    }

    private SolveResult solve(CConj constraint) {
        return SolveResult.constraints(constraint.getLeft(), constraint.getRight());
    }

    private SolveResult solve(CExists constraint) {
        final ISubstitution.Transient tsubst = PersistentSubstitution.Transient.of();
        constraint.getEVars().forEach(var -> {
            tsubst.put(var, newVar(var));
        });
        final ISubstitution.Immutable subst = tsubst.freeze();
        return SolveResult.constraints(Constraints.substitute(constraint.getConstraint(), subst));
    }

    private ITermVar newVar(ITermVar var) {
        return B.newVar(var.getResource(), fresh(var.getName()), var.getAttachments());
    }

    private SolveResult solve(CNew constraint) {
        final List<IConstraint> constraints = new ArrayList<>();
        for(ITerm scope : constraint.getNVars()) {
            constraints.add(CEqual.of(scope, newScope(scope), constraint.getMessageInfo()));
        }
        return SolveResult.constraints(constraints);
    }

    private Scope newScope(ITerm term) {
        Scope scope = M.var(v -> Scope.of(v.getResource(), fresh(v.getName()))).match(term, unifier())
                .orElseGet(() -> Scope.of("", fresh("s")).withAttachments(term.getAttachments()));
        return scope;
    }

}