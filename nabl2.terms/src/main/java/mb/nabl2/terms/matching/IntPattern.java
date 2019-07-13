package mb.nabl2.terms.matching;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

public class IntPattern extends Pattern {
    private static final long serialVersionUID = 1L;

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

    @Override protected MaybeNotInstantiatedBool matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        // @formatter:off
        return unifier.findTerm(term).match(Terms.<MaybeNotInstantiatedBool>cases()
            .integer(intTerm -> {
                return MaybeNotInstantiatedBool.ofResult(intTerm.getValue() == value);
            }).var(v -> {
                return MaybeNotInstantiatedBool.ofNotInstantiated(v);
            }).otherwise(t -> {
                return MaybeNotInstantiatedBool.ofResult(false);
            })
        );
        // @formatter:on
    }

    @Override public String toString() {
        return Integer.toString(value);
    }

}