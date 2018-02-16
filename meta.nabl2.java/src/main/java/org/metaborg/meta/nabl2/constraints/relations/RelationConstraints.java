package org.metaborg.meta.nabl2.constraints.relations;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;
import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.relations.terms.FunctionName;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

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
            M.appl4(C_EVAL, M.term(), FunctionName.matcher(), M.term(), MessageInfo.matcher(), (c, result, fun, term, origin) -> {
                return ImmutableCEvalFunction.of(result, fun, term, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.<ITerm>of(
            // @formatter:off
            build -> B.newAppl(C_BUILD_REL, build.getLeft(), build.getRelation(), build.getRight(),
                                MessageInfo.build(build.getMessageInfo())),
            check -> B.newAppl(C_CHECK_REL, check.getLeft(), check.getRelation(), check.getRight(),
                                MessageInfo.build(check.getMessageInfo())),
            eval -> B.newAppl(C_EVAL, eval.getResult(), eval.getFunction(), eval.getTerm(),
                               MessageInfo.build(eval.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IRelationConstraint substitute(IRelationConstraint constraint, IUnifier unifier) {
        return constraint.match(IRelationConstraint.Cases.<IRelationConstraint>of(
            // @formatter:off
            build -> ImmutableCBuildRelation.of(
                        unifier.findRecursive(build.getLeft()),
                        build.getRelation(),
                        unifier.findRecursive(build.getRight()),
                        build.getMessageInfo().apply(unifier::findRecursive)),
            check -> ImmutableCCheckRelation.of(
                        unifier.findRecursive(check.getLeft()),
                        check.getRelation(),
                        unifier.findRecursive(check.getRight()),
                        check.getMessageInfo().apply(unifier::findRecursive)),
            eval -> ImmutableCEvalFunction.of(
                        unifier.findRecursive(eval.getResult()),
                        eval.getFunction(),
                        unifier.findRecursive(eval.getTerm()),
                        eval.getMessageInfo().apply(unifier::findRecursive))
            // @formatter:on
        ));
    }

}