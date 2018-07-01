package mb.statix.solver;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.spec.Spec;

public interface IConstraint {

    IConstraint apply(ISubstitution.Immutable subst);

    default Iterable<Tuple2<ITerm, ITerm>> scopeExtensions(Spec spec) {
        return Iterables2.empty();
    }

    /**
     * Solve constraint
     * 
     * @param state
     *            -- monotonic from one call to the next
     * @param completeness
     * @return true is reduced, false if delayed
     * @throws InterruptedException
     * @throws Delay
     */
    Optional<Result> solve(State state, Completeness completeness, IDebugContext debug)
            throws InterruptedException, Delay;

    String toString(IUnifier unifier);

    Optional<IConstraint> cause();

    IConstraint withCause(IConstraint cause);

    static String toString(Iterable<? extends IConstraint> constraints, IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(constraint.toString(unifier));
        }
        return sb.toString();
    }

    @Value.Immutable
    abstract class AResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract List<IConstraint> constraints();

    }

}