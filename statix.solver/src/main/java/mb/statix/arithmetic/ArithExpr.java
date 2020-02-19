package mb.statix.arithmetic;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitutable;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;

public interface ArithExpr extends ISubstitutable<ArithExpr> {

    int eval(IUniDisunifier unifier) throws Delay;

    default Optional<ITerm> isTerm() {
        return Optional.empty();
    }

    String toString(TermFormatter termToString);

}