package org.metaborg.meta.nabl2.constraints.controlflow;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.ISubstitution;

public final class ControlFlowConstraints {

    private static final String CF_DIRECT_EDGE = "CFDirectEdge";

    public static IMatcher<IControlFlowConstraint> matcher() {
        return M.cases(
            // @formatter:off
            M.appl3(CF_DIRECT_EDGE, M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, scope1, scope2, origin) -> ImmutableCFDirectEdge.of(scope1, scope2, origin))
            // @formatter:on
        );
    }

    public static ITerm build(IControlFlowConstraint constraint) {
        return constraint.match(IControlFlowConstraint.Cases.<ITerm>of(
            // @formatter:off
            edge -> TB.newAppl(CF_DIRECT_EDGE, edge.getSourceNode(), edge.getTargetNode(),
                               MessageInfo.buildOnlyOriginTerm(edge.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IControlFlowConstraint substitute(IControlFlowConstraint constraint, ISubstitution.Immutable unifier) {
        return constraint.match(IControlFlowConstraint.Cases.<IControlFlowConstraint>of(
            // @formatter:off
            edge -> ImmutableCFDirectEdge.of(
                        unifier.find(edge.getSourceNode()),
                        unifier.find(edge.getTargetNode()),
                        edge.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}