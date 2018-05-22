package mb.statix.solver;

import java.util.Optional;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;

public interface IConstraint {

    IConstraint apply(Function1<ITerm, ITerm> map);

    /**
     * Solve constraint
     * 
     * @param state
     *            -- monotonic from one call to the next
     * @return true is reduced, false if delayed
     * @throws InterruptedException
     */
    Optional<Config> solve(State state) throws InterruptedException;

    String toString(IUnifier unifier);

}