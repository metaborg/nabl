package mb.nabl2.terms.matching;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class StringPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    private final String value;

    public StringPattern(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override public Set<ITermVar> getVars() {
        return ImmutableSet.of();
    }

    @Override protected MaybeNotInstantiated<Boolean> matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        // @formatter:off
        return unifier.findTerm(term).match(Terms.<MaybeNotInstantiated<Boolean>>cases()
            .string(stringTerm -> {
                return MaybeNotInstantiated.ofResult(stringTerm.getValue().equals(value));
            }).var(v -> {
                return MaybeNotInstantiated.ofNotInstantiated(v);
            }).otherwise(t -> {
                return MaybeNotInstantiated.ofResult(false);
            })
        );
        // @formatter:on
    }

    @Override public String toString() {
        return "\"" + value + "\"";
    }

}