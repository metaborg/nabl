package org.metaborg.meta.nabl2.spoofax;

import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.controlflow.terms.ImmutableCFGNode;
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
        return M.somebu(M.preserveAttachments(M.cases(
            // @formatter:off
            M.var(var -> {
                String r = (resource ==  null || var.getResource().equals(resource)) ? "" : var.getResource();
                return TB.newVar(r, var.getName());
            }),
            t -> Scope.matcher().match(t).map(s -> {
                String r = (resource == null || s.getResource().equals(resource)) ? "" : s.getResource();
                return ImmutableScope.of(r, s.getName());
            }),
            t -> CFGNode.matcher().match(t).map(n -> {
                String r = (resource == null || n.getResource().equals(resource)) ? "" : n.getResource();
                return ImmutableCFGNode.of(r, n.getName(), n.getKind());
            }),
            t -> TermIndex.matcher().match(t).map(i -> {
                String r = (resource == null || i.getResource().equals(resource)) ? "" : i.getResource();
                return ImmutableTermIndex.of(r, i.getId());
            }),
            Occurrence.matcher()
            // @formatter:on
        ))).apply(term);
    }

}