package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.CheckedTermMatch.ICheckedMatcher;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class StringPattern extends Pattern {

    private final String value;
    private final ICheckedMatcher<IStringTerm, InsufficientInstantiationException> matcher;

    public StringPattern(String value) {
        this.value = value;
        // @formatter:off
        this.matcher = CM.term(Terms.<Optional<IStringTerm>, InsufficientInstantiationException>checkedCases()
                .string(stringTerm -> {
                    return Optional.of(stringTerm);
                }).var(v -> {
                    throw new InsufficientInstantiationException(v);
                }).otherwise(t -> {
                    return Optional.empty();
                }));
        // @formatter:on
    }

    public String getValue() {
        return value;
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of();
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        final Optional<IStringTerm> stringTerm = matcher.matchOrThrow(term, unifier);
        if(!stringTerm.isPresent()) {
            return false;
        }
        return stringTerm.get().getValue().equals(value);
    }

    @Override public String toString() {
        return "\"" + value + "\"";
    }

}