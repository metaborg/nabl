package org.metaborg.meta.nabl2.spoofax;

import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableScope;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.stratego.ImmutableTermIndex;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;

public class TermSimplifier {

    public static ITerm focus(String resource, ITerm term) {
        return M.somebu(M.cases(
            // @formatter:off
            M.var(var -> {
                String r = (resource ==  null || var.getResource().equals(resource)) ? "" : var.getResource();
                return TB.newVar(r, var.getName()).withAttachments(var.getAttachments());
            }),
            t -> Scope.matcher().match(t).map(s -> {
                String r = (resource == null || s.getResource().equals(resource)) ? "" : s.getResource();
                return ImmutableScope.of(r, s.getName()).withAttachments(t.getAttachments());
            }),
            t -> TermIndex.matcher().match(t).map(i -> {
                String r = (resource == null || i.getResource().equals(resource)) ? "" : i.getResource();
                return ImmutableTermIndex.of(r, i.getId()).withAttachments(t.getAttachments());
            }),
            Occurrence.matcher()
            // @formatter:on
        )).apply(term);
    }

}