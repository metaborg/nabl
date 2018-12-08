package mb.nabl2.constraints.scopegraph;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.metaborg.util.functions.Function1;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

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

    public static IScopeGraphConstraint substitute(IScopeGraphConstraint constraint, ISubstitution.Immutable subst) {
        // @formatter:off
        return constraint.match(IScopeGraphConstraint.Cases.<IScopeGraphConstraint>of(
            decl -> ImmutableCGDecl.of(
                        subst.apply(decl.getScope()),
                        subst.apply(decl.getDeclaration()),
                        decl.getMessageInfo().apply(subst::apply)),
            ref -> ImmutableCGRef.of(
                        subst.apply(ref.getReference()),
                        subst.apply(ref.getScope()),
                        ref.getMessageInfo().apply(subst::apply)),
            edge -> ImmutableCGDirectEdge.of(
                        subst.apply(edge.getSourceScope()),
                        edge.getLabel(),
                        subst.apply(edge.getTargetScope()),
                        edge.getMessageInfo().apply(subst::apply)),
            exp -> ImmutableCGExportEdge.of(
                        subst.apply(exp.getDeclaration()),
                        exp.getLabel(),
                        subst.apply(exp.getScope()),
                        exp.getMessageInfo().apply(subst::apply)),
            imp -> ImmutableCGImportEdge.of(
                        subst.apply(imp.getScope()),
                        imp.getLabel(),
                        subst.apply(imp.getReference()),
                        imp.getMessageInfo().apply(subst::apply))
        ));
        // @formatter:on
    }

    public static IScopeGraphConstraint transform(IScopeGraphConstraint constraint, Function1<ITerm, ITerm> map) {
        // @formatter:off
        return constraint.match(IScopeGraphConstraint.Cases.<IScopeGraphConstraint>of(
            decl -> ImmutableCGDecl.of(
                        map.apply(decl.getScope()),
                        map.apply(decl.getDeclaration()),
                        decl.getMessageInfo().apply(map::apply)),
            ref -> ImmutableCGRef.of(
                        map.apply(ref.getReference()),
                        map.apply(ref.getScope()),
                        ref.getMessageInfo().apply(map::apply)),
            edge -> ImmutableCGDirectEdge.of(
                        map.apply(edge.getSourceScope()),
                        edge.getLabel(),
                        map.apply(edge.getTargetScope()),
                        edge.getMessageInfo().apply(map::apply)),
            exp -> ImmutableCGExportEdge.of(
                        map.apply(exp.getDeclaration()),
                        exp.getLabel(),
                        map.apply(exp.getScope()),
                        exp.getMessageInfo().apply(map::apply)),
            imp -> ImmutableCGImportEdge.of(
                        map.apply(imp.getScope()),
                        imp.getLabel(),
                        map.apply(imp.getReference()),
                        imp.getMessageInfo().apply(map::apply))
        ));
        // @formatter:on
    }

}