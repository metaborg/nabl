package org.metaborg.meta.nabl2.spoofax;

import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableScope;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;

public class TermSimplifier {

    public static ITerm simplify(ITerm term) {
        return M.sometd(M.cases(
            // @formatter:off
            M.var(var -> GenericTerms.newVar("", var.getName()).setAttachments(var.getAttachments())),
            t -> Scope.matcher().match(t).map(s -> ImmutableScope.of("", s.getName()).setAttachments(t.getAttachments()))
            // @formatter:on
        )).apply(term);
    }
 
}