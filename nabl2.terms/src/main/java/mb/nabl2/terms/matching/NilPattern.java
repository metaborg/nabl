package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.matching.CheckedTermMatch.ICheckedMatcher;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class NilPattern extends Pattern {

    private final ICheckedMatcher<INilTerm, InsufficientInstantiationException> matcher;

    public NilPattern() {
        // @formatter:off
        this.matcher = CM.list(ListTerms.<Optional<INilTerm>, InsufficientInstantiationException>checkedCases()
                .nil(nilTerm -> {
                    return Optional.of(nilTerm);
                }).var(v -> {
                    throw new InsufficientInstantiationException(v);
                }).otherwise(t -> {
                    return Optional.empty();
                }));
        // @formatter:on
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of();
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        return matcher.matchOrThrow(term, unifier).isPresent();
    }

    @Override public String toString() {
        return "[]";
    }

}