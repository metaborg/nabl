package mb.statix.arithmetic;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.Delay;

public interface ArithExpr {

    int eval(IUniDisunifier unifier) throws Delay;

    ArithExpr apply(ISubstitution.Immutable subst);

    default Optional<ITerm> isTerm() {
        return Optional.empty();
    }

    String toString(TermFormatter termToString);

}