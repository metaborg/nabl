package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class NilPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    public NilPattern() {
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of();
    }

    @Override protected MaybeNotInstantiated<Boolean> matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        // @formatter:off
        return M.list(listTerm -> {
            return listTerm.match(ListTerms.<MaybeNotInstantiated<Boolean>>cases()
                .nil(nilTerm -> {
                    return MaybeNotInstantiated.ofResult(true);
                }).var(v -> {
                    return MaybeNotInstantiated.ofNotInstantiated(v);
                }).otherwise(t -> {
                    return MaybeNotInstantiated.ofResult(false);
                })
            );
        }).match(unifier.findTerm(term)).orElse(MaybeNotInstantiated.ofResult(false));
        // @formatter:on
    }

    @Override public String toString() {
        return "[]";
    }

}