package mb.statix.arithmetic;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;

class TermExpr implements ArithExpr {

    private final ITerm term;

    public TermExpr(ITerm term) {
        this.term = term;
    }

    @Override public int eval(IUniDisunifier unifier) throws Delay {
        return M.integerValue().match(term, unifier).orElseThrow(() -> Delay.ofVars(unifier.getVars(term)));
    }

    @Override public ArithExpr apply(Immutable subst) {
        return new TermExpr(subst.apply(term));
    }

    @Override public Optional<ITerm> isTerm() {
        return Optional.of(term);
    }

    @Override public String toString(TermFormatter termToString) {
        return termToString.format(term);
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }


}