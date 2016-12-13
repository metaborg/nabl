package org.metaborg.meta.nabl2.constraints;

import org.metaborg.meta.nabl2.constraints.ast.ImmutableCAstProperty;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCFalse;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCTrue;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCInequal;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCDeclProperty;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGImport;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGRef;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCResolve;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCCheckRelation;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class ConstraintTerms {

    public static IMatcher<Iterable<IConstraint>> constraints() {
        return M.listElems(constraint(), (l, cs) -> cs);
    }

    public static IMatcher<IConstraint> constraint() {
        return M.cases(
            // @formatter:off
            M.appl1("CTrue", M.term(), (c, origin) -> {
                return ImmutableCTrue.of().setOriginatingTerm(origin);
            }),
            M.appl1("CFalse", originatingTerm(), (c, origin) -> {
                return ImmutableCFalse.of().setOriginatingTerm(origin);
            }),
            M.appl3("CEqual", M.term(), M.term(), originatingTerm(), (c, term1, term2, origin) -> {
                return ImmutableCEqual.of(term1, term2).setOriginatingTerm(origin);
            }),
            M.appl3("CInequal", M.term(), M.term(), originatingTerm(), (c, term1, term2, origin) -> {
                return ImmutableCInequal.of(term1, term2).setOriginatingTerm(origin);
            }),
            M.appl3("CGDecl", M.term(), M.term(), M.term(), (c, decl, scope, origin) -> {
                return ImmutableCGDecl.of(scope, decl).setOriginatingTerm(origin);
            }),
            M.appl4("CGDirectEdge", M.term(), Label.matcher(), M.term(), M.term(), (c, scope1, label, scope2, origin) -> {
                return ImmutableCGDirectEdge.of(scope1, label, scope2).setOriginatingTerm(origin);
            }),
            M.appl4("CGAssoc", M.term(), Label.matcher(), M.term(), M.term(), (c, decl, label, scope, origin) -> {
                return ImmutableCGAssoc.of(decl, label, scope).setOriginatingTerm(origin);
            }),
            M.appl4("CGNamedEdge", M.term(), Label.matcher(), M.term(), M.term(), (c, ref, label, scope, origin) -> {
                return ImmutableCGImport.of(scope, label, ref).setOriginatingTerm(origin);
            }),
            M.appl3("CGRef", M.term(), M.term(), M.term(), (c, ref, scope, origin) -> {
                return ImmutableCGRef.of(ref, scope).setOriginatingTerm(origin);
            }),
            M.appl3("CResolve", M.term(), M.term(), originatingTerm(), (c, ref, decl, origin) -> {
                return ImmutableCResolve.of(ref, decl).setOriginatingTerm(origin);
            }),
            M.appl4("CAssoc", M.term(), Label.matcher(), M.term(), originatingTerm(), (c, decl, label, scope, origin) -> {
                return ImmutableCAssoc.of(decl, label, scope).setOriginatingTerm(origin);
            }),
            M.appl5("CDeclProperty", M.term(), M.term(), M.term(), M.term(), originatingTerm(), (c, decl, key, value, prio, origin) -> {
                return ImmutableCDeclProperty.of(decl,key,value).setOriginatingTerm(origin);
            }),
            M.appl4("CBuildRel", M.term(), RelationName.matcher(), M.term(), originatingTerm(), (c, term1, rel, term2, origin) -> {
                return ImmutableCBuildRelation.of(term1, rel, term2).setOriginatingTerm(origin);
            }),
            M.appl4("CCheckRel", M.term(), RelationName.matcher(), M.term(), originatingTerm(), (c, term1, rel, term2, origin) -> {
                return ImmutableCCheckRelation.of(term1, rel, term2).setOriginatingTerm(origin);
            }),
            M.appl3("CAstProperty", TermIndex.matcher(), M.term(), M.term(), (c, index, key, value) -> {
                return ImmutableCAstProperty.of(index,key,value).setOriginatingTerm(index);
            }),
            M.term(t -> {
                ILogger logger = LoggerUtils.logger(ConstraintTerms.class);
                logger.warn("Ignoring constraint: {}", t);
                return ImmutableCTrue.of();
            })
            // @formatter:on
        );
    }

    private static IMatcher<ITerm> originatingTerm() {
        return M.appl3("Message", M.term(), M.term(), M.term(), (appl, kind, message, origin) -> {
            return origin;
        });
    }

}