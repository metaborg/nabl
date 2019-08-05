package mb.nabl2.terms.unification;

import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MaybeNotInstantiatedBool;
import mb.nabl2.terms.substitution.ISubstitution;

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
 *
 */

public interface IUnifier {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on the unifier
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Check if the unifier is finite, or whether it allows recursive terms.
     */
    boolean isFinite();

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
     * Return the representative variables of this unifier.
     */
    Set<ITermVar> repSet();

    /**
     * Return the set of free variables appearing in this unifier.
     */
    Set<ITermVar> freeVarSet();

    /**
     * Test if the unifier contains any cycles.
     */
    boolean isCyclic();
    
    /**
     * Return an unrestricted version of this unifier. An unrestricted unifier can request
     * variables from all modules at all times.
     * 
     * @throws UnsupportedOperationException
     *      If this unifier is not meant for modular/incremental solving, or if it is transient.
     */
    default IUnifier unrestricted() {
        if (isUnrestricted()) return this;
        throw new UnsupportedOperationException("Class " + getClass() + " is missing implementation for IUnifier#unrestricted");
    }
    
    /**
     * Return an restricted version of this unifier. An restricted unifier can get delays when
     * requesting variables from other modules. This is the default behavior for a unifier.
     */
    default IUnifier restricted() {
        if (!isUnrestricted()) return this;
        throw new UnsupportedOperationException("Class " + getClass() + " is missing implementation for IUnifier#restricted");
    }
    
    /**
     * Test if this unifier is unrestricted.
     * 
     * @see #unrestricted()
     */
    default boolean isUnrestricted() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on a single term
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Find the representative variable for the given variable.
     */
    ITermVar findRep(ITermVar var);

    /**
     * Return of the variable (or it representative) has a term.
     */
    boolean hasTerm(ITermVar var);

    /**
     * Find the representative term for the given term. The representative itself is not instantiated, to prevent
     * exponential blowup in time or space. If the given term is a variable, the representative term is returned, or the
     * class variable if the variable is free in the unifier. If the given term is not a variable, it is returned
     * unchanged.
     */
    ITerm findTerm(ITerm term);

    /**
     * Fully instantiate the given term according to this substitution. Instantiation may result in exponential blowup
     * of the term size. This operation preserves term sharing as much as possible. This operation throws an exception
     * on recursive terms.
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

    /**
     * Return a string representation of the given term.
     */
    String toString(ITerm term);

    /**
     * Return a string representation of the given term, up to a certain term depth.
     */
    String toString(ITerm term, int n);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on two terms
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Test if the two terms are equal relative to this unifier. If terms are incomparable under this unifier an
     * exception is thrown.
     */
    MaybeNotInstantiatedBool areEqual(ITerm term1, ITerm term2);

    public interface Immutable extends IUnifier {

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // Methods on two terms
        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Unify the two input terms. Return an updated unifier, or throw if the terms cannot be unified.
         */
        Optional<Result<Immutable>> unify(ITerm term1, ITerm term2) throws OccursException;

        Optional<Result<Immutable>> unify(ITerm term1, ITerm term2, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidVarsException;

        /**
         * Unify with the given unifier. Return an updated unifier, or throw if the terms cannot be unified.
         */
        Optional<Result<Immutable>> unify(IUnifier other) throws OccursException;

        Optional<Result<Immutable>> unify(IUnifier other, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidVarsException;

        /**
         * Return a substitution that only retains the given variable in the domain. Also returns a substitution to
         * eliminate the removed variables from terms.
         */
        Result<ISubstitution.Immutable> retain(ITermVar var);

        /**
         * Return a substitution that only retains the given variables in the domain. Also returns a substitution to
         * eliminate the removed variables from terms.
         */
        Result<ISubstitution.Immutable> retainAll(Iterable<ITermVar> vars);

        /**
         * Return a unifier with the given variable removed from the domain. Returns a substitution to eliminate the
         * variable from terms.
         */
        Result<ISubstitution.Immutable> remove(ITermVar var);

        /**
         * Return a unifier with the given variables removed from the domain. Returns a substitution to eliminate the
         * variable from terms.
         */
        Result<ISubstitution.Immutable> removeAll(Iterable<ITermVar> vars);

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
        Optional<Immutable> unify(ITerm term1, ITerm term2) throws OccursException;

        Optional<Immutable> unify(ITerm term1, ITerm term2, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidVarsException;

        /**
         * Unify with the given unifier. Return a diff unifier, or throw if the terms cannot be unified.
         */
        Optional<Immutable> unify(IUnifier other) throws OccursException;

        Optional<Immutable> unify(IUnifier other, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidVarsException;

        /**
         * Retain only the given variable in the domain of this unifier. Returns a substitution to eliminate the removed
         * variables from terms.
         */
        ISubstitution.Immutable retain(ITermVar var);

        /**
         * Retain only the given variables in the domain of this unifier. Returns a substitution to eliminate the
         * removed variables from terms.
         */
        ISubstitution.Immutable retainAll(Iterable<ITermVar> vars);

        /**
         * Remove the given variable from the domain of this unifier. Returns a substitution to eliminate the variable
         * from terms.
         */
        ISubstitution.Immutable remove(ITermVar var);

        /**
         * Remove the given variables from the domain of this unifier. Returns a substitution to eliminate the variable
         * from terms.
         */
        ISubstitution.Immutable removeAll(Iterable<ITermVar> vars);

        /**
         * Return immutable version of this unifier. The transient unifier cannot be used anymore after this call.
         */
        Immutable freeze();

    }

}