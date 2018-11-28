package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.CheckedTermMatch.ICheckedMatcher;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class IntPattern extends Pattern {

    private final int value;
    private final ICheckedMatcher<IIntTerm, InsufficientInstantiationException> matcher;

    public IntPattern(int value) {
        this.value = value;
        // @formatter:off
        this.matcher = CM.term(Terms.<Optional<IIntTerm>, InsufficientInstantiationException>checkedCases()
                .integer(intTerm -> {
                    return Optional.of(intTerm);
                }).var(v -> {
                    throw new InsufficientInstantiationException(v);
                }).otherwise(t -> {
                    return Optional.empty();
                }));
        // @formatter:on
    }

    public int getValue() {
        return value;
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of();
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        final Optional<IIntTerm> intTerm = matcher.matchOrThrow(term, unifier);
        if(!intTerm.isPresent()) {
            return false;
        }
        return intTerm.get().getValue() == value;
    }

    @Override public String toString() {
        return Integer.toString(value);
    }

}