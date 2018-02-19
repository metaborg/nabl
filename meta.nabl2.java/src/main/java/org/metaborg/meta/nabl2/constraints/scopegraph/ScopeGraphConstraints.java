package org.metaborg.meta.nabl2.constraints.scopegraph;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;
import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

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
            decl -> B.newAppl(CG_DECL, decl.getDeclaration(), decl.getScope(),
                               MessageInfo.buildOnlyOriginTerm(decl.getMessageInfo())),
            ref -> B.newAppl(CG_REF, ref.getReference(), ref.getScope(),
                              MessageInfo.buildOnlyOriginTerm(ref.getMessageInfo())),
            edge -> B.newAppl(CG_DIRECT_EDGE, edge.getSourceScope(), edge.getLabel(), edge.getTargetScope(),
                               MessageInfo.buildOnlyOriginTerm(edge.getMessageInfo())),
            exp -> B.newAppl(CG_EXPORT_EDGE, exp.getDeclaration(), exp.getLabel(), exp.getScope(),
                              MessageInfo.buildOnlyOriginTerm(exp.getMessageInfo())),
            imp -> B.newAppl(CG_IMPORT_EDGE, imp.getReference(), imp.getLabel(), imp.getScope(),
                              MessageInfo.buildOnlyOriginTerm(imp.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IScopeGraphConstraint substitute(IScopeGraphConstraint constraint, IUnifier unifier) {
        return constraint.match(IScopeGraphConstraint.Cases.<IScopeGraphConstraint>of(
            // @formatter:off
            decl -> ImmutableCGDecl.of(
                        unifier.findRecursive(decl.getScope()),
                        unifier.findRecursive(decl.getDeclaration()),
                        decl.getMessageInfo().apply(unifier::findRecursive)),
            ref -> ImmutableCGRef.of(
                        unifier.findRecursive(ref.getReference()),
                        unifier.findRecursive(ref.getScope()),
                        ref.getMessageInfo().apply(unifier::findRecursive)),
            edge -> ImmutableCGDirectEdge.of(
                        unifier.findRecursive(edge.getSourceScope()),
                        edge.getLabel(),
                        unifier.findRecursive(edge.getTargetScope()),
                        edge.getMessageInfo().apply(unifier::findRecursive)),
            exp -> ImmutableCGExportEdge.of(
                        unifier.findRecursive(exp.getDeclaration()),
                        exp.getLabel(),
                        unifier.findRecursive(exp.getScope()),
                        exp.getMessageInfo().apply(unifier::findRecursive)),
            imp -> ImmutableCGImportEdge.of(
                        unifier.findRecursive(imp.getScope()),
                        imp.getLabel(),
                        unifier.findRecursive(imp.getReference()),
                        imp.getMessageInfo().apply(unifier::findRecursive))
            // @formatter:on
        ));
    }

}