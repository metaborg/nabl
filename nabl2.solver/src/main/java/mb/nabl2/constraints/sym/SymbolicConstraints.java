package mb.nabl2.constraints.sym;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

public final class SymbolicConstraints {

    private static final String C_FACT = "CFact";
    private static final String C_GOAL = "CGoal";

    public static IMatcher<ISymbolicConstraint> matcher() {
        return M.<ISymbolicConstraint>cases(
            // @formatter:off
            M.appl2(C_FACT, M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, fact, origin) -> {
                return ImmutableCFact.of(fact, origin);
            }),
            M.appl2(C_GOAL, M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, goal, origin) -> {
                return ImmutableCGoal.of(goal, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(ISymbolicConstraint constraint) {
        return constraint.match(ISymbolicConstraint.Cases.<ITerm>of(
            // @formatter:off
            fact -> B.newAppl(C_FACT, fact.getFact(), MessageInfo.buildOnlyOriginTerm(fact.getMessageInfo())),
            goal ->  B.newAppl(C_GOAL, goal.getGoal(), MessageInfo.buildOnlyOriginTerm(goal.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static ISymbolicConstraint substitute(ISymbolicConstraint constraint, ISubstitution.Immutable subst) {
        return constraint.match(ISymbolicConstraint.Cases.<ISymbolicConstraint>of(
            // @formatter:off
            fact -> ImmutableCFact.of(
                        subst.apply(fact.getFact()),
                        fact.getMessageInfo().apply(subst::apply)),
            goal ->  ImmutableCGoal.of(
                        subst.apply(goal.getGoal()),
                        goal.getMessageInfo().apply(subst::apply))
            // @formatter:on
        ));
    }

}