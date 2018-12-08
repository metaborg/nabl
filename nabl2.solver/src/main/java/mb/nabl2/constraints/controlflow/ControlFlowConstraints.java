package mb.nabl2.constraints.controlflow;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

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
            M.appl5(C_TF_APPL, CFGNode.matcher(), M.stringValue(), M.stringValue(), M.integerValue(), M.listElems(),
                    (c, index, propname, modname, offset, args) -> ImmutableCTFAppl.of(index, propname, modname, offset, args, MessageInfo.of(index)))
            // @formatter:on
        );
    }

    public static ITerm build(IControlFlowConstraint constraint) {
        return constraint.match(IControlFlowConstraint.Cases.<ITerm>of(
        // @formatter:off
            edge -> B.newAppl(CF_DIRECT_EDGE, edge.getSourceNode(), edge.getTargetNode(),
                               MessageInfo.buildOnlyOriginTerm(edge.getMessageInfo())),
            tfAppl -> B.newAppl(C_TF_APPL, tfAppl.getCFGNode(), B.newString(tfAppl.getPropertyName()),
                                B.newInt(tfAppl.getOffset()), B.newList(tfAppl.getArguments()))
        ));
    }

    public static IControlFlowConstraint substitute(IControlFlowConstraint constraint, ISubstitution.Immutable subst) {
        // @formatter:off
        return constraint.match(IControlFlowConstraint.Cases.<IControlFlowConstraint>of(
            edge -> ImmutableCFDirectEdge.of(
                        subst.apply(edge.getSourceNode()),
                        subst.apply(edge.getTargetNode()),
                        edge.getMessageInfo().apply(subst::apply)),
            tfAppl -> ImmutableCTFAppl.of(
                    tfAppl.getCFGNode(),
                    tfAppl.getPropertyName(),
                    tfAppl.getModuleName(),
                    tfAppl.getOffset(),
                    tfAppl.getArguments().stream().map(subst::apply).collect(Collectors.toList()),
                    tfAppl.getMessageInfo().apply(subst::apply))
        ));
        // @formatter:on
    }

    public static IControlFlowConstraint transform(IControlFlowConstraint constraint, Function1<ITerm, ITerm> map) {
        // @formatter:off
        return constraint.match(IControlFlowConstraint.Cases.<IControlFlowConstraint>of(
            edge -> ImmutableCFDirectEdge.of(
                        map.apply(edge.getSourceNode()),
                        map.apply(edge.getTargetNode()),
                        edge.getMessageInfo().apply(map::apply)),
            tfAppl -> ImmutableCTFAppl.of(
                    tfAppl.getCFGNode(),
                    tfAppl.getPropertyName(),
                    tfAppl.getModuleName(),
                    tfAppl.getOffset(),
                    tfAppl.getArguments().stream().map(map::apply).collect(Collectors.toList()),
                    tfAppl.getMessageInfo().apply(map::apply))
        ));
        // @formatter:on
    }

}