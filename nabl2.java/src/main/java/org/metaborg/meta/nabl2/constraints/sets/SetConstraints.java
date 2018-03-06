package org.metaborg.meta.nabl2.constraints.sets;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;
import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.sets.SetTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

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

    public static ISetConstraint substitute(ISetConstraint constraint, IUnifier unifier) {
        return constraint.match(ISetConstraint.Cases.of(
            // @formatter:off
            subseteq -> ImmutableCSubsetEq.of(
                            unifier.findRecursive(subseteq.getLeft()),
                            unifier.findRecursive(subseteq.getRight()),
                            subseteq.getProjection(),
                            subseteq.getMessageInfo().apply(unifier::findRecursive)),
            distinct -> ImmutableCDistinct.of(
                            unifier.findRecursive(distinct.getSet()),
                            distinct.getProjection(),
                            distinct.getMessageInfo().apply(unifier::findRecursive)),
            eval -> ImmutableCEvalSet.of(
                            unifier.findRecursive(eval.getResult()),
                            unifier.findRecursive(eval.getSet()),
                            eval.getMessageInfo().apply(unifier::findRecursive))
            // @formatter:on
        ));
    }

}