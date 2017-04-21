package org.metaborg.meta.nabl2.constraints.sets;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.sets.SetTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

public final class SetConstraints {

    private static final String C_SUBSET_EQ = "CSubsetEq";
    private static final String C_DISTINCT = "CDistinct";

    public static IMatcher<ISetConstraint> matcher() {
        return M.<ISetConstraint>cases(
            // @formatter:off
            M.appl4(C_SUBSET_EQ, M.term(), SetTerms.projectionMatcher(), M.term(), MessageInfo.matcher(), (c, left, proj, right, origin) -> {
                return ImmutableCSubsetEq.of(left, right, proj, origin);
            }),
            M.appl3(C_DISTINCT, SetTerms.projectionMatcher(), M.term(), MessageInfo.matcher(), (c, proj, set, origin) -> {
                return ImmutableCDistinct.of(set, proj, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(ISetConstraint constraint) {
        return constraint.match(ISetConstraint.Cases.<ITerm>of(
            // @formatter:off
            subseteq -> TB.newAppl(C_SUBSET_EQ, subseteq.getLeft(), SetTerms.buildProjection(subseteq.getProjection()),
                                   subseteq.getRight(), MessageInfo.build(subseteq.getMessageInfo())),
            distinct -> TB.newAppl(C_DISTINCT, SetTerms.buildProjection(distinct.getProjection()), distinct.getSet(),
                                   MessageInfo.build(distinct.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static ISetConstraint find(ISetConstraint constraint, IUnifier unifier) {
        return constraint.match(ISetConstraint.Cases.of(
            // @formatter:off
            subseteq -> ImmutableCSubsetEq.of(
                            unifier.find(subseteq.getLeft()),
                            unifier.find(subseteq.getRight()),
                            subseteq.getProjection(),
                            subseteq.getMessageInfo().apply(unifier::find)),
            distinct -> ImmutableCDistinct.of(
                            unifier.find(distinct.getSet()),
                            distinct.getProjection(),
                            distinct.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}