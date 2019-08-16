package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Set;

import com.google.common.collect.ImmutableMultimap;
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

    @Override protected MaybeNotInstantiatedBool matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        // @formatter:off
        return unifier.findTerm(term).match(Terms.<MaybeNotInstantiatedBool>cases()
            .string(stringTerm -> {
                return MaybeNotInstantiatedBool.ofResult(stringTerm.getValue().equals(value));
            }).var(v -> {
                return MaybeNotInstantiatedBool.ofNotInstantiated(v);
            }).otherwise(t -> {
                return MaybeNotInstantiatedBool.ofResult(false);
            })
        );
        // @formatter:on
    }

    @Override public ITerm asTerm(ImmutableMultimap.Builder<ITermVar, ITerm> equalities) {
        return B.newString(value);
    }

    @Override public String toString() {
        return "\"" + value + "\"";
    }

}