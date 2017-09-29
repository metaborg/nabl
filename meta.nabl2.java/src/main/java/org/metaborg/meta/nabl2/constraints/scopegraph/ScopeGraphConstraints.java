package org.metaborg.meta.nabl2.constraints.scopegraph;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.ISubstitution;

public final class ScopeGraphConstraints {

    private static final String CG_DECL = "CGDecl";
    private static final String CG_DIRECT_EDGE = "CGDirectEdge";
    private static final String CG_EXPORT_EDGE = "CGAssoc";
    private static final String CG_IMPORT_EDGE = "CGNamedEdge";
    private static final String CG_REF = "CGRef";

    public static IMatcher<IScopeGraphConstraint> matcher() {
        return M.<IScopeGraphConstraint>cases(
            // @formatter:off
            M.appl3(CG_DECL, M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, decl, scope, origin) -> {
                        return ImmutableCGDecl.of(scope, decl, origin);
                    }),
            M.appl3(CG_REF, M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, ref, scope, origin) -> {
                        return ImmutableCGRef.of(ref, scope, origin);
                    }),
            M.appl4(CG_DIRECT_EDGE, M.term(), Label.matcher(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, scope1, label, scope2, origin) -> {
                        return ImmutableCGDirectEdge.of(scope1, label, scope2, origin);
                    }),
            M.appl4(CG_EXPORT_EDGE, M.term(), Label.matcher(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, decl, label, scope, origin) -> {
                        return ImmutableCGExportEdge.of(decl, label, scope, origin);
                    }),
            M.appl4(CG_IMPORT_EDGE, M.term(), Label.matcher(), M.term(), MessageInfo.matcherOnlyOriginTerm(),
                    (c, ref, label, scope, origin) -> {
                        return ImmutableCGImportEdge.of(scope, label, ref, origin);
                    })
            // @formatter:on
        );
    }

    public static ITerm build(IScopeGraphConstraint constraint) {
        return constraint.match(IScopeGraphConstraint.Cases.<ITerm>of(
            // @formatter:off
            decl -> TB.newAppl(CG_DECL, decl.getDeclaration(), decl.getScope(),
                               MessageInfo.buildOnlyOriginTerm(decl.getMessageInfo())),
            ref -> TB.newAppl(CG_REF, ref.getReference(), ref.getScope(),
                              MessageInfo.buildOnlyOriginTerm(ref.getMessageInfo())),
            edge -> TB.newAppl(CG_DIRECT_EDGE, edge.getSourceScope(), edge.getLabel(), edge.getTargetScope(),
                               MessageInfo.buildOnlyOriginTerm(edge.getMessageInfo())),
            exp -> TB.newAppl(CG_EXPORT_EDGE, exp.getDeclaration(), exp.getLabel(), exp.getScope(),
                              MessageInfo.buildOnlyOriginTerm(exp.getMessageInfo())),
            imp -> TB.newAppl(CG_IMPORT_EDGE, imp.getReference(), imp.getLabel(), imp.getScope(),
                              MessageInfo.buildOnlyOriginTerm(imp.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IScopeGraphConstraint substitute(IScopeGraphConstraint constraint, ISubstitution.Immutable unifier) {
        return constraint.match(IScopeGraphConstraint.Cases.<IScopeGraphConstraint>of(
            // @formatter:off
            decl -> ImmutableCGDecl.of(
                        unifier.find(decl.getScope()),
                        unifier.find(decl.getDeclaration()),
                        decl.getMessageInfo().apply(unifier::find)),
            ref -> ImmutableCGRef.of(
                        unifier.find(ref.getReference()),
                        unifier.find(ref.getScope()),
                        ref.getMessageInfo().apply(unifier::find)),
            edge -> ImmutableCGDirectEdge.of(
                        unifier.find(edge.getSourceScope()),
                        edge.getLabel(),
                        unifier.find(edge.getTargetScope()),
                        edge.getMessageInfo().apply(unifier::find)),
            exp -> ImmutableCGExportEdge.of(
                        unifier.find(exp.getDeclaration()),
                        exp.getLabel(),
                        unifier.find(exp.getScope()),
                        exp.getMessageInfo().apply(unifier::find)),
            imp -> ImmutableCGImportEdge.of(
                        unifier.find(imp.getScope()),
                        imp.getLabel(),
                        unifier.find(imp.getReference()),
                        imp.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}