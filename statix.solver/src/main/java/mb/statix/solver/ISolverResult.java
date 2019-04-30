package mb.statix.solver;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;

/**
 * Interface for the solver result.
 */
public interface ISolverResult {
    /**
     * @return
     *      a set of constraints that encountered errors
     */
    Set<IConstraint> errors();

    /**
     * @return
     *      true if there are errors, false otherwise
     */
    default boolean hasErrors() {
        return !errors().isEmpty();
    }
    
    /**
     * @return
     *      true if there are delays, false otherwise
     */
    default boolean hasDelays() {
        return !delays().isEmpty();
    }

    /**
     * A map from constraints to the delay on which that constraint is delayed.
     * 
     * @return
     *      the map of delays of this solver result
     */
    Map<IConstraint, Delay> delays();

    /**
     * Creates a new Delay on all the variables and critical edges in the delays of this solver
     * result.
     * 
     * @return
     *      the new delay
     */
    default Delay delay() {
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        ImmutableSet.Builder<CriticalEdge> scopes = ImmutableSet.builder();
        delays().values().stream().forEach(d -> {
            vars.addAll(d.vars());
            scopes.addAll(d.criticalEdges());
        });
        return new Delay(vars.build(), scopes.build(), null, null);
    }
    
    /**
     * Creates a copy of this solver result by replacing the {@link #delays()} map with the
     * specified map.
     * Nulls are not permitted as keyrs or values.
     * 
     * @param entries
     *      the entries to be set as the delays map
     * 
     * @return
     *      a modified copy of this solver result
     */
    ISolverResult withDelays(Map<? extends IConstraint, ? extends Delay> entries);
    
    /**
     * Creates a copy of this solver result by replacing the {@link #errors()} set with the
     * specified elements.
     * 
     * @param elements
     *      the constraints to be set
     * 
     * @return
     *      a modified copy of this solver result
     */
    ISolverResult withErrors(Iterable<? extends IConstraint> elements);
    
    /**
     * @return
     *      the unifier of the final result
     */
    IUnifier.Immutable unifier();
}
