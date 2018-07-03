package mb.nabl2.constraints.controlflow;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.stream.Collectors;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.controlflow.terms.CFGNode;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

public final class ControlFlowConstraints {

    private static final String CF_DIRECT_EDGE = "CFDirectEdge";
    private static final String C_TF_APPL = "CTFAppl";

    public static IMatcher<IControlFlowConstraint> matcher() {
        return M.cases(
            // @formatter:off
            M.appl3(CF_DIRECT_EDGE, M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, node1, node2, origin) -> ImmutableCFDirectEdge.of(node1, node2, origin)),
            M.appl4(C_TF_APPL, CFGNode.matcher(), M.stringValue(), M.integerValue(), M.listElems(), 
                    (c, index, propname, offset, args) -> ImmutableCTFAppl.of(index, propname, offset, args, MessageInfo.of(index)))
            // @formatter:on
        );
    }

    public static ITerm build(IControlFlowConstraint constraint) {
        return constraint.match(IControlFlowConstraint.Cases.<ITerm>of(
            // @formatter:off
            edge -> B.newAppl(CF_DIRECT_EDGE, edge.getSourceNode(), edge.getTargetNode(),
                               MessageInfo.buildOnlyOriginTerm(edge.getMessageInfo())),
            tfAppl -> B.newAppl(C_TF_APPL, tfAppl.getCFGNode(), B.newString(tfAppl.getPropertyName()), B.newInt(tfAppl.getOffset()), B.newList(tfAppl.getArguments()))
            // @formatter:on
        ));
    }

    public static IControlFlowConstraint substitute(IControlFlowConstraint constraint, ISubstitution.Immutable subst) {
        return constraint.match(IControlFlowConstraint.Cases.<IControlFlowConstraint>of(
            // @formatter:off
            edge -> ImmutableCFDirectEdge.of(
                        subst.apply(edge.getSourceNode()),
                        subst.apply(edge.getTargetNode()),
                        edge.getMessageInfo().apply(subst::apply)),
            tfAppl -> ImmutableCTFAppl.of(
                    tfAppl.getCFGNode(),
                    tfAppl.getPropertyName(),
                    tfAppl.getOffset(),
                    tfAppl.getArguments().stream().map(subst::apply).collect(Collectors.toList()),
                    tfAppl.getMessageInfo().apply(subst::apply))
            // @formatter:on
        ));
    }

}