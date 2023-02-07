package mb.nabl2.terms.unification.ud;

import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Predicate1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.IReplacement;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.u.IUnifier;

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
 *
 */


public interface IUniDisunifier extends mb.nabl2.terms.unification.u.IUnifier {

    /**
     * Return a unifier that makes these terms equal, relative to the current unifier.
     *
     * If no result is returned, the terms are unequal. Otherwise, if an empty unifier is returned, the terms are equal.
     * Finally, if a non-empty unifier is returned, the terms are not equal, but can be made equal by the returned
     * unifier.
     */
    @Override Optional<IUnifier.Immutable> diff(ITerm term1, ITerm term2);

    Set.Immutable<Diseq> disequalities();


    public interface Immutable extends IUniDisunifier, mb.nabl2.terms.unification.u.IUnifier.Immutable {

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // Methods on two terms
        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Unify the two input terms. Return an updated unifier, or throw if the terms cannot be unified.
         */
        @Override default Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(ITerm term1, ITerm term2)
                throws OccursException {
            try {
                return unify(term1, term2, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /**
         * Unify the two input terms. Return an updated unifier, or throw if the terms cannot be unified.
         */
        @Override Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(ITerm term1, ITerm term2,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException;

        /**
         * Unify with the given unifier. Return an updated unifier, or throw if the terms cannot be unified.
         */
        @Override default Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(IUnifier other)
                throws OccursException {
            try {
                return unify(other, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /**
         * Unify with the given unifier. Return an updated unifier, or throw if the terms cannot be unified.
         */
        @Override Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(IUnifier other,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException;

        /**
         * Unify and disunify with the given unifier. Return an updated unifier, or throw if the terms cannot be
         * unified.
         */
        default Optional<IUniDisunifier.Result<IUnifier.Immutable>> uniDisunify(IUniDisunifier other)
                throws OccursException {
            try {
                return uniDisunify(other, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /**
         * Unify and disunify with the given unifier. Return an updated unifier, or throw if the terms cannot be
         * unified.
         */
        Optional<IUniDisunifier.Result<IUnifier.Immutable>> uniDisunify(IUniDisunifier other,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException;

        /**
         * Unify the two term pairs. Return a diff unifier, or throw if the terms cannot be unified.
         */
        @Override default Optional<IUniDisunifier.Result<IUnifier.Immutable>>
                unify(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) throws OccursException {
            try {
                return unify(equalities, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(
                Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException;

        /**
         * Disunify the two input terms. Returns empty if disunify failed, otherwise returns a unifier representing the
         * reduced inequality.
         */
        default Optional<IUniDisunifier.Result<Optional<Diseq>>> disunify(Iterable<ITermVar> universal, ITerm term1,
                ITerm term2) {
            try {
                return disunify(universal, term1, term2, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /**
         * Disunify the two input terms. Returns empty if disunify failed, otherwise returns a unifier representing the
         * reduced inequality.
         */
        Optional<IUniDisunifier.Result<Optional<Diseq>>> disunify(Iterable<ITermVar> universal, ITerm term1,
                ITerm term2, Predicate1<ITermVar> isRigid) throws RigidException;

        /**
         * Disunify the given unifier. Returns empty if disunify failed, otherwise returns a unifier representing the
         * reduced inequality.
         */
        Optional<IUniDisunifier.Result<Optional<Diseq>>> disunify(Iterable<ITermVar> universal,
                IUnifier.Immutable diseqs, Predicate1<ITermVar> isRigid) throws RigidException;

        /**
         * Disunify the two input terms. Returns empty if disunify failed, otherwise returns a unifier representing the
         * reduced inequality.
         */
        default Optional<IUniDisunifier.Result<Optional<Diseq>>> disunify(ITerm term1, ITerm term2) {
            return disunify(CapsuleUtil.immutableSet(), term1, term2);
        }

        /**
         * Disunify the two input terms. Returns empty if disunify failed, otherwise returns a unifier representing the
         * reduced inequality.
         */
        default Optional<IUniDisunifier.Result<Optional<Diseq>>> disunify(ITerm term1, ITerm term2,
                Predicate1<ITermVar> isRigid) throws RigidException {
            return disunify(CapsuleUtil.immutableSet(), term1, term2, isRigid);
        }

        /**
         * Return a substitution that only retains the given variable in the domain. Also returns a substitution to
         * eliminate the removed variables from terms.
         */
        @Override IUniDisunifier.Result<ISubstitution.Immutable> retain(ITermVar var);

        /**
         * Return a substitution that only retains the given variables in the domain. Also returns a substitution to
         * eliminate the removed variables from terms.
         */
        @Override IUniDisunifier.Result<ISubstitution.Immutable> retainAll(Set.Immutable<ITermVar> vars);

        /**
         * Return a unifier with the given variable removed from the domain. Returns a substitution to eliminate the
         * variable from terms.
         */
        @Override IUniDisunifier.Result<ISubstitution.Immutable> remove(ITermVar var);

        /**
         * Return a unifier with the given variables removed from the domain. Returns a substitution to eliminate the
         * variable from terms. Note that removal never unifies terms or variables that were not already unified before.
         */
        @Override IUniDisunifier.Result<ISubstitution.Immutable> removeAll(Set.Immutable<ITermVar> vars);

        /**
         * Apply a variable renaming to this unifier.
         */
        @Override IUniDisunifier.Immutable rename(IRenaming renaming);

        /**
         * Apply a term replacement to this unifier.
         *
         * Please note that replacement might not respect all original disequalities, due to normalization.
         */
        @Override IUniDisunifier.Immutable replace(IReplacement replacement);

        /**
         * Return transient version of this unifier.
         */
        @Override IUniDisunifier.Transient melt();

    }

    /**
     * Interface that gives a result and an updated immutable unifier.
     */
    public interface Result<T> extends mb.nabl2.terms.unification.u.IUnifier.Result<T> {

        @Override T result();

        @Override Immutable unifier();

    }

    public interface Transient extends IUniDisunifier, mb.nabl2.terms.unification.u.IUnifier.Transient {

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        // Methods on two terms
        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Unify the two input terms. Return a diff unifier, or throw if the terms cannot be unified.
         */
        @Override default Optional<IUnifier.Immutable> unify(ITerm term1, ITerm term2) throws OccursException {
            try {
                return unify(term1, term2, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override Optional<IUnifier.Immutable> unify(ITerm term1, ITerm term2, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException;

        /**
         * Unify with the given unifier. Return a diff unifier, or throw if the terms cannot be unified.
         */
        default Optional<IUnifier.Immutable> unify(IUniDisunifier other) throws OccursException {
            try {
                return unify(other, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        Optional<IUnifier.Immutable> unify(IUniDisunifier other, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException;

        /**
         * Unify the two term pairs. Return a diff unifier, or throw if the terms cannot be unified.
         */
        @Override default Optional<IUnifier.Immutable>
                unify(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) throws OccursException {
            try {
                return unify(equalities, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override Optional<IUnifier.Immutable> unify(
                Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException;

        /**
         * Disunify with the given unifier. Return whether it succeeded.
         */
        default Optional<Optional<Diseq>> disunify(Iterable<ITermVar> universal, ITerm term1, ITerm term2) {
            try {
                return disunify(universal, term1, term2, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        Optional<Optional<Diseq>> disunify(Iterable<ITermVar> universal, ITerm term1, ITerm term2,
                Predicate1<ITermVar> isRigid) throws RigidException;

        default Optional<Optional<Diseq>> disunify(Iterable<ITermVar> universal, IUnifier.Immutable diseqs) {
            try {
                return disunify(universal, diseqs, Predicate1.never());
            } catch(RigidException ex) {
                throw new IllegalStateException(ex);
            }
        }

        Optional<Optional<Diseq>> disunify(Iterable<ITermVar> universal, IUnifier.Immutable diseqs,
                Predicate1<ITermVar> isRigid) throws RigidException;

        default Optional<Optional<Diseq>> disunify(ITerm term1, ITerm term2) {
            return disunify(CapsuleUtil.immutableSet(), term1, term2);
        }

        default Optional<Optional<Diseq>> disunify(ITerm term1, ITerm term2, Predicate1<ITermVar> isRigid)
                throws RigidException {
            return disunify(CapsuleUtil.immutableSet(), term1, term2, isRigid);
        }

        /**
         * Retain only the given variable in the domain of this unifier. Returns a substitution to eliminate the removed
         * variables from terms.
         */
        @Override ISubstitution.Immutable retain(ITermVar var);

        /**
         * Retain only the given variables in the domain of this unifier. Returns a substitution to eliminate the
         * removed variables from terms.
         */
        @Override ISubstitution.Immutable retainAll(Iterable<ITermVar> vars);

        /**
         * Remove the given variable from the domain of this unifier. Returns a substitution to eliminate the variable
         * from terms.
         */
        @Override ISubstitution.Immutable remove(ITermVar var);

        /**
         * Remove the given variables from the domain of this unifier. Returns a substitution to eliminate the variable
         * from terms.
         */
        @Override ISubstitution.Immutable removeAll(Iterable<ITermVar> vars);

        /**
         * Return immutable version of this unifier. The transient unifier cannot be used anymore after this call.
         */
        @Override IUniDisunifier.Immutable freeze();

    }

}