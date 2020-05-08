package mb.nabl2.constraints.sym;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.metaborg.util.functions.Function1;

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
                return CFact.of(fact, origin);
            }),
            M.appl2(C_GOAL, M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, goal, origin) -> {
                return CGoal.of(goal, origin);
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
        // @formatter:off
        return constraint.match(ISymbolicConstraint.Cases.<ISymbolicConstraint>of(
            fact -> CFact.of(
                        subst.apply(fact.getFact()),
                        fact.getMessageInfo().apply(subst::apply)),
            goal ->  CGoal.of(
                        subst.apply(goal.getGoal()),
                        goal.getMessageInfo().apply(subst::apply))
        ));
        // @formatter:on
    }

    public static ISymbolicConstraint transform(ISymbolicConstraint constraint, Function1<ITerm, ITerm> map) {
        // @formatter:off
        return constraint.match(ISymbolicConstraint.Cases.<ISymbolicConstraint>of(
            fact -> CFact.of(
                        map.apply(fact.getFact()),
                        fact.getMessageInfo().apply(map::apply)),
            goal ->  CGoal.of(
                        map.apply(goal.getGoal()),
                        goal.getMessageInfo().apply(map::apply))
        ));
        // @formatter:on
    }

}