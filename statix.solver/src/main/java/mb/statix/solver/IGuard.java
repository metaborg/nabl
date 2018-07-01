package mb.statix.solver;

import java.util.Optional;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;

public interface IGuard {

    IGuard apply(ISubstitution.Immutable subst);

    /**
     * Solve constraint
     * 
     * @param state
     *            -- monotonic from one call to the next
     * @return new state if reduced, or none
     * @throws InterruptedException
     */
    Optional<State> solve(State state, IDebugContext debug) throws InterruptedException, Delay;

    String toString(IUnifier unifier);

    static String toString(Iterable<? extends IGuard> constraints, IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IGuard constraint : constraints) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(constraint.toString(unifier));
        }
        return sb.toString();
    }

}