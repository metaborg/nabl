package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class StringPattern extends Pattern {

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

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        // @formatter:off
        return CM.<Boolean, InsufficientInstantiationException>cases(
            CM.string(stringTerm -> {
                if(stringTerm.getValue().equals(value)) {
                    return true;
                } else {
                    return false;
                }
            }),
            CM.var(v -> {
                throw new InsufficientInstantiationException(v);
            })
        ).matchOrThrow(term, unifier).orElse(false);
        // @formatter:on
    }

    @Override public String toString() {
        return "\"" + value + "\"";
    }

}