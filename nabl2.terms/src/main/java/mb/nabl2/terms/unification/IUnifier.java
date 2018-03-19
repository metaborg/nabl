package mb.nabl2.terms.unification;

import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

/**
 * Unification
 * 
 * The following should hold:
 * 
 * <code>
 *   if (d', U') = U.unify(_, _) then U.compose(d') == U'
 *   !U.remove(v).varSet().contains(v)
 *   if U.varSet().contains(v) then !U.remove(v).freeVarSet().contains(v)
 *   Sets.intersection(U.varSet(), U.freeVarSet()).isEmpty()
 *   if (d', U') = U.remove(v), and t' = d'.findRecursive(t) then U.findRecursive(t) == U'.findRecursive(t')
 *   if (d', U') = U.remove(v) then U'.compose(d') == U
 * </code>
 * 
 * Internal invariants:
 * 
 * <code>
 *   terms.values().noneMatch(t -> t instanceOf ITermVar)
 *   Sets.intersection(reps.keySet(), terms.keySet()).isEmpty()
 * </code>
 * 
 * Support for recursive terms is easy to add, but makes many operations exceptional. For example: remove(ITermVar),
 * findRecursive(ITerm).
 */

public interface IUnifier {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on the unifier
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if the substitution is empty.
     */
    boolean isEmpty();

    /**
     * Test if the unifier contains a substitution for the given variable.
     */
    boolean contains(ITermVar var);

    /**
     * Return the size of this substitution.
     */
    int size();

    /**
     * Return the domain of this unifier.
     */
    Set<ITermVar> varSet();

    /**
     * Return the set of free variables appearing in this unifier.
     */
    Set<ITermVar> freeVarSet();

    /**
     * Test if the unifier contains any cycles.
     */
    boolean isCyclic();

    /**
     * Test if this unifier entails the given unifier.
     * 
     * TODO: how to implement this? do we need to explicitly specify local variables?
     */
    // boolean entails(IUnifier unifier);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on a single term
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Find the representative variable for the given variable.
     */
    ITermVar findRep(ITermVar var);

    /**
     * Find the representative term for the given term. The representative itself is not instantiated, to prevent
     * exponential blowup in time or space. If the given term is a variable, the representative term is returned, or the
     * class variable if the variable is free in the unifier. If the given term is not a variable, it is returned
     * unchanged.
     */
    ITerm findTerm(ITerm term);

    /**
     * Fully instantiate the given term according to this substitution. Instantiation may result in exponential blowup
     * of the term size. This operation preserves term sharing as much as possible.
     */
    ITerm findRecursive(ITerm term);

    /**
     * Test if the given term is ground relative to this unifier.
     */
    boolean isGround(ITerm term);

    /**
     * Test if the given term is cyclic relative to this unifier.
     */
    boolean isCyclic(ITerm term);

    /**
     * Return the set of variables that appear in the given term relative to this unifier.
     */
    Set<ITermVar> getVars(ITerm term);

    /**
     * Return the size of the given term relative to this unifier.
     */
    TermSize size(ITerm term);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on two terms
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Test if the two terms are equal relative to this unifier. Further unification preserves equality.
     */
    boolean areEqual(ITerm term1, ITerm term2);

    /**
     * Test if the two terms are unequal relative to this unifier. Further unification preserves inequality.
     */
    boolean areUnequal(ITerm term1, ITerm term2);

    public interface Immutable extends IUnifier {

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // Methods on two terms
        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Unify the two input terms. Return an updated unifier, or throw if the terms cannot be unified.
         */
        Result<Immutable> unify(ITerm term1, ITerm term2) throws UnificationException;

        /**
         * Unify with the given unifier. Return an updated unifier, or throw if the terms cannot be unified.
         */
        Result<Immutable> unify(IUnifier other) throws UnificationException;

        /**
         * Return the composition of this unifier with another unifier.
         */
        Immutable compose(IUnifier other);

        /**
         * Match the term against the given pattern. Return assignments for the variables in the pattern, or throw if
         * the match fails.
         */
        Result<Immutable> match(ITerm pattern, ITerm term) throws MatchException;

        /**
         * Return a substituion that only retains the given variable in the domain. Also returns a substitution to
         * eliminate the removed variables from terms.
         */
        Result<Immutable> retain(ITermVar var);

        /**
         * Return a substituion that only retains the given variables in the domain. Also returns a substitution to
         * eliminate the removed variables from terms.
         */
        Result<Immutable> retainAll(Iterable<ITermVar> vars);

        /**
         * Return a unifier with the given variable removed from the domain. Returns a substitution to eliminate the
         * variable from terms.
         */
        Result<Immutable> remove(ITermVar var);

        /**
         * Return a unifier with the given variables removed from the domain. Returns a substitution to eliminate the
         * variable from terms.
         */
        Result<Immutable> removeAll(Iterable<ITermVar> vars);

        /**
         * Return transient version of this unifier.
         */
        Transient melt();

        /**
         * Interface that gives a result and an updated immutable unifier.
         */
        public interface Result<T> {

            T result();

            Immutable unifier();

        }

    }

    public interface Transient extends IUnifier {

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // Methods on two terms
        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Unify the two input terms. Return a diff unifier, or throw if the terms cannot be unified.
         */
        Immutable unify(ITerm term1, ITerm term2) throws UnificationException;

        /**
         * Unify with the given unifier. Return a diff unifier, or throw if the terms cannot be unified.
         */
        Immutable unify(IUnifier other) throws UnificationException;

        /**
         * Compose this unifier with another unifier.
         */
        void compose(IUnifier other);

        /**
         * Match the term against the given pattern. Return assignments for the variables in the pattern, or throw if
         * the match fails.
         */
        Immutable match(ITerm pattern, ITerm term) throws MatchException;

        /**
         * Retain only the given variable in the domain of this unifier. Returns a substitution to eliminate the removed
         * variables from terms.
         */
        Immutable retain(ITermVar var);

        /**
         * Retain only the given variables in the domain of this unifier. Returns a substitution to eliminate the
         * removed variables from terms.
         */
        Immutable retainAll(Iterable<ITermVar> vars);

        /**
         * Remove the given variable from the domain of this unifier. Returns a substitution to eliminate the variable
         * from terms.
         */
        Immutable remove(ITermVar var);

        /**
         * Remove the given variables from the domain of this unifier. Returns a substitution to eliminate the variable
         * from terms.
         */
        Immutable removeAll(Iterable<ITermVar> vars);

        /**
         * Return immutable version of this unifier. The transient unifier cannot be used anymore after this call.
         */
        Immutable freeze();

    }

}