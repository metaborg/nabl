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

    @Override protected MaybeNotInstantiated<Boolean> matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        // @formatter:off
        return unifier.findTerm(term).match(Terms.<MaybeNotInstantiated<Boolean>>cases()
            .integer(intTerm -> {
                return MaybeNotInstantiated.ofResult(intTerm.getValue() == value);
            }).var(v -> {
                return MaybeNotInstantiated.ofNotInstantiated(v);
            }).otherwise(t -> {
                return MaybeNotInstantiated.ofResult(false);
            })
        );
        // @formatter:on
    }

    @Override public String toString() {
        return Integer.toString(value);
    }

}