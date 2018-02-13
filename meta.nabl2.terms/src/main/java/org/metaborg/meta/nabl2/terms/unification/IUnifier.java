package org.metaborg.meta.nabl2.terms.unification;

import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

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
         * Remove the given variable from the unifier.
         */
        Immutable remove(ITermVar var);

        /**
         * Remove the given variables from the unifier.
         */
        Immutable removeAll(Iterable<ITermVar> vars);

        /**
         * Match the term against the given pattern. Return assignments for the variables in the pattern, or throw if
         * the match fails.
         */
        Result<Immutable> match(ITerm pattern, ITerm term) throws MatchException;

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
         * Remove the given variable from the unifier. Returns a substitution that eliminates the given variable keeping
         * in sync with this unifier.
         */
        Immutable remove(ITermVar var);

        /**
         * Remove the given variables from the unifier. Returns a substitution that eliminates the given variables
         * keeping in sync with this unifier.
         * 
         */
        Immutable removeAll(Iterable<ITermVar> vars);

        /**
         * Match the term against the given pattern. Return assignments for the variables in the pattern, or throw if
         * the match fails.
         */
        Immutable match(ITerm pattern, ITerm term) throws MatchException;

        /**
         * Return immutable version of this unifier. The transient unifier cannot be used anymore after this call.
         */
        Immutable freeze();

    }

}