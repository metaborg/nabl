package org.metaborg.meta.nabl2.constraints;

import org.metaborg.meta.nabl2.constraints.base.ImmutableCFalse;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCTrue;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCInequal;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGImport;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGRef;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCPropertyOf;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCResolve;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

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
            M.appl4("CGDirectEdge", M.term(), M.term(), M.term(), M.term(), (c, scope1, label, scope2, origin) -> {
                return ImmutableCGDirectEdge.of(scope1, label, scope2).setOriginatingTerm(origin);
            }),
            M.appl4("CGAssoc", M.term(), M.term(), M.term(), M.term(), (c, decl, label, scope, origin) -> {
                return ImmutableCGAssoc.of(decl, label, scope).setOriginatingTerm(origin);
            }),
            M.appl4("CGNamedEdge", M.term(), M.term(), M.term(), M.term(), (c, ref, label, scope, origin) -> {
                return ImmutableCGImport.of(scope, label, ref).setOriginatingTerm(origin);
            }),
            M.appl3("CGRef", M.term(), M.term(), M.term(), (c, ref, scope, origin) -> {
                return ImmutableCGRef.of(ref, scope).setOriginatingTerm(origin);
            }),
            M.appl3("CResolve", M.term(), M.term(), originatingTerm(), (c, ref, decl, origin) -> {
                return ImmutableCResolve.of(ref, decl).setOriginatingTerm(origin);
            }),
            M.appl4("CAssoc", M.term(), M.term(), M.term(), originatingTerm(), (c, decl, label, scope, origin) -> {
                return ImmutableCAssoc.of(decl, label, scope).setOriginatingTerm(origin);
            }),
            M.appl5("CPropertyOf", M.term(), M.term(), M.term(), M.term(), originatingTerm(), (c, decl, key, value, prio, origin) -> {
                return ImmutableCPropertyOf.of(decl,key,value).setOriginatingTerm(origin);
            }),
            M.term(t -> ImmutableCTrue.of())
            // @formatter:on
        );
    }

    private static IMatcher<ITerm> originatingTerm() {
        return M.appl3("Message", M.term(), M.term(), M.term(), (appl, kind, message, origin) -> {
            return origin;
        });
    }

}