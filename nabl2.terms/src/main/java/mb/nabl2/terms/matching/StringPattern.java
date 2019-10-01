package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Set;

import org.metaborg.util.functions.Action2;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier.Immutable;

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

    @Override protected boolean matchTerm(ITerm term, Transient subst, Immutable unifier, Eqs eqs) {
        // @formatter:off
        return unifier.findTerm(term).match(Terms.<Boolean>cases()
            .string(stringTerm -> {
                return stringTerm.getValue().equals(value);
            }).var(v -> {
                eqs.add(v, this);
                return true;
            }).otherwise(t -> {
                return false;
            })
        );
        // @formatter:on
    }

    @Override
    protected ITerm asTerm(Action2<ITermVar, ITerm> equalities) {
        return B.newString(value);
    }

    @Override public String toString() {
        return "\"" + value + "\"";
    }

}