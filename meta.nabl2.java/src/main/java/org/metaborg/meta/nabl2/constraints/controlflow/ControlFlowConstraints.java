package org.metaborg.meta.nabl2.constraints.controlflow;

import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.ISubstitution;
import org.metaborg.meta.nabl2.unification.IUnifier;

public final class ControlFlowConstraints {

    private static final String CF_DECL = "CFDecl";
    private static final String CF_DIRECT_EDGE = "CFDirectEdge";
    private static final String CF_DECL_PROPERTY = "CFDeclProperty";

    public static IMatcher<IControlFlowConstraint> matcher() {
        return M.cases(
            // @formatter:off
            M.appl3(CF_DECL, M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, decl, scope, origin) -> ImmutableCFDecl.of(scope, decl, origin)),
            M.appl3(CF_DIRECT_EDGE, M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, scope1, scope2, origin) -> ImmutableCFDirectEdge.of(scope1, scope2, origin)),
            M.appl5(CF_DECL_PROPERTY, M.term(), M.term(), M.term(), Constraints.priorityMatcher(), MessageInfo.matcher(),
                    (c, decl, key, value, prio, origin) -> ImmutableCFDeclProperty.of(decl, key, value, prio, origin))
            // @formatter:on
        );
    }

    public static ITerm build(IControlFlowConstraint constraint) {
        return constraint.match(IControlFlowConstraint.Cases.<ITerm>of(
            // @formatter:off
            decl -> TB.newAppl(CF_DECL, decl.getDeclaration(), decl.getNode(),
                               MessageInfo.buildOnlyOriginTerm(decl.getMessageInfo())),
            edge -> TB.newAppl(CF_DIRECT_EDGE, edge.getSourceNode(), edge.getTargetNode(),
                               MessageInfo.buildOnlyOriginTerm(edge.getMessageInfo())),
            prop ->TB.newAppl(CF_DECL_PROPERTY, prop.getDeclaration(), prop.getKey(), prop.getValue(),
                              Constraints.buildPriority(prop.getPriority()), MessageInfo.build(prop.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IControlFlowConstraint substitute(IControlFlowConstraint constraint, ISubstitution.Immutable unifier) {
        return constraint.match(IControlFlowConstraint.Cases.<IControlFlowConstraint>of(
            // @formatter:off
            decl -> ImmutableCFDecl.of(
                        unifier.find(decl.getNode()),
                        unifier.find(decl.getDeclaration()),
                        decl.getMessageInfo().apply(unifier::find)),
            edge -> ImmutableCFDirectEdge.of(
                        unifier.find(edge.getSourceNode()),
                        unifier.find(edge.getTargetNode()),
                        edge.getMessageInfo().apply(unifier::find)),
            prop -> ImmutableCFDeclProperty.of(
                        unifier.find(prop.getDeclaration()),
                        prop.getKey(),
                        unifier.find(prop.getValue()),
                        prop.getPriority(),
                        prop.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}