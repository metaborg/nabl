package mb.statix.arithmetic;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;

class TermExpr implements ArithExpr, Serializable {

    private static final long serialVersionUID = 1L;

    private final ITerm term;

    public TermExpr(ITerm term) {
        this.term = term;
    }

    @Override public int eval(IUniDisunifier unifier) throws Delay {
        return M.integerValue().match(term, unifier).orElseThrow(() -> Delay.ofVars(unifier.getVars(term)));
    }

    @Override public ArithExpr apply(ISubstitution.Immutable subst) {
        return new TermExpr(subst.apply(term));
    }

    @Override public ArithExpr apply(IRenaming subst) {
        return new TermExpr(subst.apply(term));
    }

    @Override public Optional<ITerm> isTerm() {
        return Optional.of(term);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return term.getVars();
    }

    @Override public String toString(TermFormatter termToString) {
        return termToString.format(term);
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        TermExpr termExpr = (TermExpr) o;
        return Objects.equals(term, termExpr.term);
    }

    @Override public int hashCode() {
        return Objects.hash(term);
    }
}
