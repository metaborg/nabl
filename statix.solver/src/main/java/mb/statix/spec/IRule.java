package mb.statix.spec;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;

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
        return params().stream().flatMap(t -> t.getVars().stream()).collect(ImmutableSet.toImmutableSet());
    }

    /**
     * @return
     *      the constraint that makes up the body of this rule
     */
    IConstraint body();

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
     * @see #apply(List, IUnifier, IConstraint)
     */
    default Optional<Tuple2<ISubstitution.Immutable,IConstraint>> apply(List<? extends ITerm> args, IUnifier unifier) throws Delay {
        return apply(args, unifier, null);
    }
    
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
    Optional<Tuple2<ISubstitution.Immutable,IConstraint>> apply(List<? extends ITerm> args, IUnifier unifier, @Nullable IConstraint cause) throws Delay;
    
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
