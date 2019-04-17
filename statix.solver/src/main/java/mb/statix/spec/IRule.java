package mb.statix.spec;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;
import mb.statix.taico.solver.IMState;

public interface IRule {

    /**
     * @return
     *      the name of this rule
     */
    String name();

    /**
     * @return
     *      the list of parameter patterns
     */
    List<Pattern> params();

    /**
     * @return
     *      the set of variables that occur in the parameters
     */
    default Set<ITermVar> paramVars() {
        return params().stream().flatMap(t -> t.getVars().stream()).collect(Collectors.toSet());
    }

    /**
     * @return
     *      the set of variables specified for the body
     */
    Set<ITermVar> bodyVars();

    /**
     * @return
     *      the list of constraints that make up the body of this rule
     */
    List<IConstraint> body();

    /**
     * @param spec
     *      the spec
     * 
     * @return
     *      true if this rule always holds, false otherwise
     * 
     * @throws InterruptedException
     */
    Optional<Boolean> isAlways(Spec spec) throws InterruptedException;

    /**
     * @param subst
     *      the substitution to apply
     * 
     * @return
     *      a copy of this rule with the given substitution applied to the body
     */
    IRule apply(ISubstitution.Immutable subst);

    /**
     * Applies the given arguments to this rule.
     * 
     * @param args
     *      the arguments to apply
     * @param state
     *      the current state
     * 
     * @return
     *      a tuple with the new state, new variables and the set of new constraints. If the
     *      arguments do not match the parameters, an empty optional is returned
     * 
     * @throws Delay
     *      If the arguments cannot be matched to the parameters of this rule because one or more
     *      terms are not ground.
     */
    Optional<Tuple3<State, Set<ITermVar>, Set<IConstraint>>> apply(List<ITerm> args, State state) throws Delay;
    
    /**
     * Applies the given arguments to this rule.
     * 
     * @param args
     *      the arguments to apply
     * @param state
     *      the current state
     * 
     * @return
     *      a tuple with the new variables and the set of new constraints. If the
     *      arguments do not match the parameters, an empty optional is returned
     * 
     * @throws Delay
     *      If the arguments cannot be matched to the parameters of this rule because one or more
     *      terms are not ground.
     * @throws IllegalStateException
     *      If this rule is a module boundary.
     */
    Optional<Tuple2<Set<ITermVar>, Set<IConstraint>>> apply(List<ITerm> args, IMState state) throws Delay;

    /**
     * Formats this rule where constraints are formatted with the given TermFormatter.
     * 
     * @param termToString
     *      the term formatter to format constraints with
     * 
     * @return
     *      the string
     */
    String toString(TermFormatter termToString);
}
