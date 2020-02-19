package mb.statix.arithmetic;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
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

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        return term.getVars().elementSet();
    }

    @Override public ArithExpr doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new TermExpr(totalSubst.apply(term));
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