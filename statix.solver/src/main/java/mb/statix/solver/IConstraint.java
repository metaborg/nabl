package mb.statix.solver;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.spec.Spec;

public interface IConstraint {

    IConstraint apply(Function1<ITerm, ITerm> map);

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
     */
    Optional<Result> solve(State state, Completeness completeness, IDebugContext debug) throws InterruptedException;

    String toString(IUnifier unifier);

    @Value.Immutable
    abstract class AResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract Set<IConstraint> constraints();

    }

}