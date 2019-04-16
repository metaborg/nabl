package mb.statix.solver;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.spec.Spec;
import mb.statix.taico.solver.MConstraintContext;
import mb.statix.taico.solver.MConstraintResult;
import mb.statix.taico.solver.MState;

/**
 * Interface to represent a constraint.
 */
public interface IConstraint {

    /**
     * Applies the given substitution to this constraint.
     * 
     * @param subst
     *      the substitution
     * 
     * @return
     *      a copy of this constraint with the given substitution applied
     */
    IConstraint apply(ISubstitution.Immutable subst);

    default Collection<CriticalEdge> criticalEdges(Spec spec) {
        return ImmutableList.of();
    }

    /**
     * Return the terms that are used as constraint arguments.
     *
     * @return Constraint argument terms.
     */
    default Iterable<ITerm> terms() {
        return Iterables2.empty();
    }

    /**
     * Solves this constraint.
     * 
     * @param state
     *            -- monotonic from one call to the next
     * @param params
     *      the context containing info about completeness, rigid and closed as well as debug
     * 
     * @return
     *      true if reduced, false if delayed
     * 
     * @throws InterruptedException
     *      Optional exception that is thrown when solving this constraint is interrupted.
     *      
     * @throws Delay
     *      If this constraint cannot be solved in the current state with the given context.
     *      The exception contains the information about what information is required to solve.
     */
    Optional<ConstraintResult> solve(State state, ConstraintContext params) throws InterruptedException, Delay;
    
    /**
     * Solves this constraint with mutable state.
     * 
     * @param state
     *      mutable state
     * @param params
     *      the context containing info about completeness, rigid and closed as well as debug
     *      
     * @return
     *      true is reduced, false if delayed
     * 
     * @throws InterruptedException
     *      Optional exception that is thrown when solving this constraint is interrupted.
     *      
     * @throws Delay
     *      If this constraint cannot be solved in the current state with the given context.
     *      The exception contains the information about what information is required to solve.
     */
    Optional<MConstraintResult> solve(MState state, MConstraintContext params) throws InterruptedException, Delay;
    
    /**
     * @return
     *      true if this constraint could cause state modifications, false otherwise
     */
    default boolean canModifyState() {
        return false;
    }

    /**
     * Converts this constraint to a string, where terms are formatted using the given term
     * formatter.
     * 
     * @param termToString
     *      the term formatter for formatting terms in this constraint
     * 
     * @return
     *      the string
     */
    String toString(TermFormatter termToString);

    /**
     * @return
     *      the constraint that caused this constraint to be added
     */
    Optional<IConstraint> cause();

    /**
     * Creates a copy of the current constraint with the given cause set as cause.
     * 
     * @param cause
     *      the cause
     * 
     * @return
     *      the copied constraint
     */
    IConstraint withCause(IConstraint cause);

    /**
     * Converts the given constraints to a comma separated string, using the given TermFormatter to
     * format the terms in each constraint.
     * 
     * @param constraints
     *      the constraints
     * @param termToString
     *      the term formatter
     * 
     * @return
     *      the string
     */
    static String toString(Iterable<? extends IConstraint> constraints, TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(constraint.toString(termToString));
        }
        return sb.toString();
    }

    @Value.Immutable
    static abstract class AConstraintResult {

        @Value.Parameter public abstract State state();

        @Value.Parameter public abstract List<IConstraint> constraints();

        @Value.Parameter public abstract List<ITermVar> vars();

        public static ConstraintResult of(State state) {
            return ConstraintResult.of(state, ImmutableList.of(), ImmutableList.of());
        }

        public static ConstraintResult ofConstraints(State state, IConstraint... constraints) {
            return ofConstraints(state, Arrays.asList(constraints));
        }

        public static ConstraintResult ofConstraints(State state, Iterable<? extends IConstraint> constraints) {
            return ConstraintResult.of(state, ImmutableList.copyOf(constraints), ImmutableList.of());
        }

        public static ConstraintResult ofVars(State state, Iterable<? extends ITermVar> vars) {
            return ConstraintResult.of(state, ImmutableList.of(), ImmutableList.copyOf(vars));
        }

    }

}