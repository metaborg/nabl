package org.metaborg.meta.nabl2.constraints;

import org.metaborg.meta.nabl2.constraints.base.ImmutableFalse;
import org.metaborg.meta.nabl2.constraints.base.ImmutableTrue;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableEqual;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableInequal;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableImport;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableRef;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableResolve;
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
                return ImmutableTrue.of().setOriginatingTerm(origin);
            }),
            M.appl1("CFalse", originatingTerm(), (c, origin) -> {
                return ImmutableFalse.of().setOriginatingTerm(origin);
            }),
            M.appl3("CEqual", M.term(), M.term(), originatingTerm(), (c, term1, term2, origin) -> {
                return ImmutableEqual.of(term1, term2).setOriginatingTerm(origin);
            }),
            M.appl3("CInequal", M.term(), M.term(), originatingTerm(), (c, term1, term2, origin) -> {
                return ImmutableInequal.of(term1, term2).setOriginatingTerm(origin);
            }),
            M.appl3("CGDecl", M.term(), M.term(), M.term(), (c, decl, scope, origin) -> {
                return ImmutableDecl.of(scope, decl).setOriginatingTerm(origin);
            }),
            M.appl4("CGDirectEdge", M.term(), M.term(), M.term(), M.term(), (c, scope1, label, scope2, origin) -> {
                return ImmutableDirectEdge.of(scope1, label, scope2).setOriginatingTerm(origin);
            }),
            M.appl4("CGAssoc", M.term(), M.term(), M.term(), M.term(), (c, decl, label, scope, origin) -> {
                return ImmutableAssoc.of(decl, label, scope).setOriginatingTerm(origin);
            }),
            M.appl4("CGNamedEdge", M.term(), M.term(), M.term(), M.term(), (c, ref, label, scope, origin) -> {
                return ImmutableImport.of(scope, label, ref).setOriginatingTerm(origin);
            }),
            M.appl3("CGRef", M.term(), M.term(), M.term(), (c, ref, scope, origin) -> {
                return ImmutableRef.of(ref, scope).setOriginatingTerm(origin);
            }),
            M.appl3("CResolve", M.term(), M.term(), originatingTerm(), (c, ref, decl, origin) -> {
                return ImmutableResolve.of(ref, decl).setOriginatingTerm(origin);
            }),
            M.term(t -> ImmutableTrue.of())
            // @formatter:on
        );
    }

    private static IMatcher<ITerm> originatingTerm() {
        return M.appl3("Message", M.term(), M.term(), M.term(), (appl, kind, message, origin) -> {
            return origin;
        });
    }

}