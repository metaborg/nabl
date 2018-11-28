package mb.nabl2.terms.matching;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class IntPattern extends Pattern {

    private final int value;

    public IntPattern(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of();
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        // @formatter:off
        return unifier.findTerm(term).matchOrThrow(Terms.<Boolean, InsufficientInstantiationException>checkedCases()
            .integer(intTerm -> {
                if(intTerm.getValue() == value) {
                    return true;
                } else {
                    return false;
                }
            }).var(v -> {
                throw new InsufficientInstantiationException(v);
            }).otherwise(t -> {
                return false;
            })
        );
        // @formatter:on
    }

    @Override public String toString() {
        return Integer.toString(value);
    }

}