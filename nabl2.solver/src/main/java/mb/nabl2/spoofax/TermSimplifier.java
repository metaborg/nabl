package mb.nabl2.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.terms.ITerm;
import static mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.stratego.TermIndex;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;

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
                return Scope.of(r, s.getName());
            }),
            (t, u) -> TermIndex.matcher().match(t, u).map(i -> {
                String r = (resource == null || i.getResource().equals(resource)) ? "" : i.getResource();
                return TermIndex.of(r, i.getId());
            }),
            Occurrence.matcher()
        ))::match).apply(term);
        // @formatter:on
    }

}
