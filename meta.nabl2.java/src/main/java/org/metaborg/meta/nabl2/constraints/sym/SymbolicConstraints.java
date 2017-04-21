package org.metaborg.meta.nabl2.constraints.sym;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

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
            fact -> TB.newAppl(C_FACT, fact.getFact(), MessageInfo.buildOnlyOriginTerm(fact.getMessageInfo())),
            goal ->  TB.newAppl(C_GOAL, goal.getGoal(), MessageInfo.buildOnlyOriginTerm(goal.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static ISymbolicConstraint find(ISymbolicConstraint constraint, IUnifier unifier) {
        return constraint.match(ISymbolicConstraint.Cases.<ISymbolicConstraint>of(
            // @formatter:off
            fact -> ImmutableCFact.of(
                        unifier.find(fact.getFact()),
                        fact.getMessageInfo().apply(unifier::find)),
            goal ->  ImmutableCGoal.of(
                        unifier.find(goal.getGoal()),
                        goal.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}