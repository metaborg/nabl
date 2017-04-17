package org.metaborg.meta.nabl2.constraints.relations;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

public final class RelationConstraints {

    private static final String C_BUILD_REL = "CBuildRel";
    private static final String C_CHECK_REL = "CCheckRel";
    private static final String C_EVAL = "CEval";

    public static IMatcher<IRelationConstraint> matcher() {
        return M.<IRelationConstraint>cases(
            // @formatter:off
            M.appl4(C_BUILD_REL, M.term(), RelationName.matcher(), M.term(), MessageInfo.matcher(), (c, term1, rel, term2, origin) -> {
                return ImmutableCBuildRelation.of(term1, rel, term2, origin);
            }),
            M.appl4(C_CHECK_REL, M.term(), RelationName.matcher(), M.term(), MessageInfo.matcher(), (c, term1, rel, term2, origin) -> {
                return ImmutableCCheckRelation.of(term1, rel, term2, origin);
            }),
            M.appl4(C_EVAL, M.term(), RelationTerms.functionName(), M.term(), MessageInfo.matcher(), (c, result, fun, term, origin) -> {
                return ImmutableCEvalFunction.of(result, fun, term, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.<ITerm>of(
            // @formatter:off
            build -> TB.newAppl(C_BUILD_REL, build.getLeft(), build.getRelation(), build.getRight(),
                                MessageInfo.build(build.getMessageInfo())),
            check -> TB.newAppl(C_CHECK_REL, check.getLeft(), check.getRelation(), check.getRight(),
                                MessageInfo.build(check.getMessageInfo())),
            eval -> TB.newAppl(C_EVAL, eval.getResult(), TB.newString(eval.getFunction()), eval.getTerm(),
                               MessageInfo.build(eval.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IRelationConstraint find(IRelationConstraint constraint, IUnifier unifier) {
        return constraint.match(IRelationConstraint.Cases.<IRelationConstraint>of(
            // @formatter:off
            build -> ImmutableCBuildRelation.of(
                        unifier.find(build.getLeft()),
                        build.getRelation(),
                        unifier.find(build.getRight()),
                        build.getMessageInfo().apply(unifier::find)),
            check -> ImmutableCCheckRelation.of(
                        unifier.find(check.getLeft()),
                        check.getRelation(),
                        unifier.find(check.getRight()),
                        check.getMessageInfo().apply(unifier::find)),
            eval -> ImmutableCEvalFunction.of(
                        unifier.find(eval.getResult()),
                        eval.getFunction(),
                        unifier.find(eval.getTerm()),
                        eval.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}