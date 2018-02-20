package org.metaborg.meta.nabl2.constraints.controlflow;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;
import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

public final class ControlFlowConstraints {

    private static final String CF_DIRECT_EDGE = "CFDirectEdge";
    private static final String C_TF_APPL = "CTFAppl";

    public static IMatcher<IControlFlowConstraint> matcher() {
        return M.cases(
            // @formatter:off
            M.appl3(CF_DIRECT_EDGE, M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, scope1, scope2, origin) -> ImmutableCFDirectEdge.of(scope1, scope2, origin)),
            M.appl4(C_TF_APPL, TermIndex.matcher(), M.stringValue(), M.integerValue(), M.listElems(), 
                    (c, index, propname, offset, args) -> ImmutableCTFAppl.of(index, propname, offset, args, MessageInfo.of(index)))
            // @formatter:on
        );
    }

    public static ITerm build(IControlFlowConstraint constraint) {
        return constraint.match(IControlFlowConstraint.Cases.<ITerm>of(
            // @formatter:off
            edge -> B.newAppl(CF_DIRECT_EDGE, edge.getSourceNode(), edge.getTargetNode(),
                               MessageInfo.buildOnlyOriginTerm(edge.getMessageInfo())),
            tfAppl -> B.newAppl(C_TF_APPL, tfAppl.getIndex(), B.newString(tfAppl.getPropertyName()), B.newInt(tfAppl.getOffset()), B.newList(tfAppl.getArguments()))
            // @formatter:on
        ));
    }

    public static IControlFlowConstraint substitute(IControlFlowConstraint constraint, IUnifier unifier) {
        return constraint.match(IControlFlowConstraint.Cases.<IControlFlowConstraint>of(
            // @formatter:off
            edge -> ImmutableCFDirectEdge.of(
                        unifier.findRecursive(edge.getSourceNode()),
                        unifier.findRecursive(edge.getTargetNode()),
                        edge.getMessageInfo().apply(unifier::findRecursive)),
            tfAppl -> ImmutableCTFAppl.of(
                    tfAppl.getIndex(),
                    tfAppl.getPropertyName(),
                    tfAppl.getOffset(),
                    tfAppl.getArguments().stream().map(unifier::findRecursive).collect(Collectors.toList()),
                    tfAppl.getMessageInfo().apply(unifier::findRecursive))
            // @formatter:on
        ));
    }

}