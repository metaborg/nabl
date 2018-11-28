package mb.nabl2.terms.matching;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class NilPattern extends Pattern {

    public NilPattern() {
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of();
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        // @formatter:off
        return unifier.findTerm(term).matchOrThrow(Terms.<Boolean, InsufficientInstantiationException>checkedCases()
            .list(listTerm -> {
                return listTerm.matchOrThrow(ListTerms.<Boolean, InsufficientInstantiationException>checkedCases()
                    .nil(nilTerm -> {
                        return true;
                    }).var(v -> {
                        throw new InsufficientInstantiationException(v);
                    }).otherwise(t -> false)
                );
            }).var(v -> {
                throw new InsufficientInstantiationException(v);
            }).otherwise(t -> {
                return false;
            })
        );
        // @formatter:on
    }

    @Override public String toString() {
        return "[]";
    }

}