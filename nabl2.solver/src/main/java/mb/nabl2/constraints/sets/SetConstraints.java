package mb.nabl2.constraints.sets;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.sets.SetTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

public final class SetConstraints {

    private static final String C_SUBSET_EQ = "CSubsetEq";
    private static final String C_DISTINCT = "CDistinct";
    private static final String C_EVAL_SET = "CEvalSet";

    public static IMatcher<ISetConstraint> matcher() {
        return M.<ISetConstraint>cases(
        // @formatter:off
            M.appl4(C_SUBSET_EQ, M.term(), SetTerms.projectionMatcher(), M.term(), MessageInfo.matcher(), (c, left, proj, right, origin) -> {
                return ImmutableCSubsetEq.of(left, right, proj, origin);
            }),
            M.appl3(C_DISTINCT, SetTerms.projectionMatcher(), M.term(), MessageInfo.matcher(), (c, proj, set, origin) -> {
                return ImmutableCDistinct.of(set, proj, origin);
            }),
            M.appl3(C_EVAL_SET, M.term(), M.term(), MessageInfo.matcher(), (c, result, set, origin) -> {
                return ImmutableCEvalSet.of(result, set, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(ISetConstraint constraint) {
        return constraint.match(ISetConstraint.Cases.<ITerm>of(
        // @formatter:off
            subseteq -> B.newAppl(C_SUBSET_EQ, subseteq.getLeft(), SetTerms.buildProjection(subseteq.getProjection()),
                                   subseteq.getRight(), MessageInfo.build(subseteq.getMessageInfo())),
            distinct -> B.newAppl(C_DISTINCT, SetTerms.buildProjection(distinct.getProjection()), distinct.getSet(),
                                   MessageInfo.build(distinct.getMessageInfo())),
            eval -> B.newAppl(C_EVAL_SET, eval.getResult(), eval.getSet(), MessageInfo.build(eval.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static ISetConstraint substitute(ISetConstraint constraint, ISubstitution.Immutable unifier) {
        // @formatter:off
        return constraint.match(ISetConstraint.Cases.of(
            subseteq -> ImmutableCSubsetEq.of(
                            unifier.apply(subseteq.getLeft()),
                            unifier.apply(subseteq.getRight()),
                            subseteq.getProjection(),
                            subseteq.getMessageInfo().apply(unifier::apply)),
            distinct -> ImmutableCDistinct.of(
                            unifier.apply(distinct.getSet()),
                            distinct.getProjection(),
                            distinct.getMessageInfo().apply(unifier::apply)),
            eval -> ImmutableCEvalSet.of(
                            unifier.apply(eval.getResult()),
                            unifier.apply(eval.getSet()),
                            eval.getMessageInfo().apply(unifier::apply))
        ));
        // @formatter:on
    }

}