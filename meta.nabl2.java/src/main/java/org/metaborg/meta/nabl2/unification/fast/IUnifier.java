package org.metaborg.meta.nabl2.unification.fast;

import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

public interface IUnifier {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on the unifier
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Return the size of this substitution.
     */
    int size();

    /**
     * Return the domain of this unifier.
     */
    Set<ITermVar> varSet();

    /**
     * Test if this unifier entails the given unifier.
     * 
     * TODO: how to implement this? do we need to explicitly specify local variables?
     */
    // boolean entails(IUnifier unifier);

    /**
     * Return a unifier restricted to the
     * 
     * TODO: what if a in vars, and a |-> t(b), but b not in vars, and i) b is free, or ii) b is an open term, or iii) b
     * is a ground term?
     */
    // IUnifier restrict(Set<ITermVar> vars);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods on a single term
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Find the representative for the given term. The representative itself is not instantiated, to prevent exponential
     * blowup in time or space. If the given term is a variable, the representative term is returned, or the class
     * variable if the variable is free in the unifier. If the given term is not a variable, it is returned unchanged.
     */
    ITerm find(ITerm term);

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
        Tuple2<Set<ITermVar>, IUnifier> unify(ITerm term1, ITerm term2) throws UnificationException;

        /**
         * Match the term against the given pattern. Return assignments for the variables in the pattern, or throw if
         * the match fails.
         */
        // Tuple2<Map<ITermVar, ITerm>, IUnifier> match(ITerm pattern, ITerm term) throws UnificationException;

        /**
         * Return transient version of this unifier.
         */
        Transient melt();

    }

    public interface Transient extends IUnifier {

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // Methods on two terms
        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Unify the two input terms. Return an updated unifier, or throw if the terms cannot be unified.
         */
        Set<ITermVar> unify(ITerm term1, ITerm term2) throws UnificationException;

        /**
         * Match the term against the given pattern. Return assignments for the variables in the pattern, or throw if
         * the match fails.
         */
        // Map<ITermVar, ITerm> match(ITerm pattern, ITerm term) throws UnificationException;

        /**
         * Return immutable version of this unifier. The transient unifier cannot be used anymore after this call.
         */
        Immutable freeze();

    }

}