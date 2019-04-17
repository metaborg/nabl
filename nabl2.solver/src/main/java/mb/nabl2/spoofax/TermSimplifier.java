package mb.nabl2.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.scopegraph.terms.ImmutableScope;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.stratego.ImmutableTermIndex;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.Transform.T;

public class TermSimplifier {

    public static ITerm focus(String resource, ITerm term) {
        // @formatter:off
        return T.somebu(M.preserveAttachments(M.cases(
            M.var(var -> {
                String r = (resource ==  null || var.getResource().equals(resource)) ? "" : var.getResource();
                return B.newVar(r, var.getName());
            }),
            (t, u) -> Scope.matcher().match(t, u).map(s -> {
                String r = (resource == null || s.getResource().equals(resource)) ? "" : s.getResource();
                return ImmutableScope.of(r, s.getName());
            }),
            (t, u) -> TermIndex.matcher().match(t, u).map(i -> {
                String r = (resource == null || i.getResource().equals(resource)) ? "" : i.getResource();
                return ImmutableTermIndex.of(r, i.getId());
            }),
            Occurrence.matcher()
        ))::match).apply(term);
        // @formatter:on
    }

}
