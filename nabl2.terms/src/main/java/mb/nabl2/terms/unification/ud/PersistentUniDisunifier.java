package mb.nabl2.terms.unification.ud;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Predicate1;
import org.spoofax.terms.util.NotImplementedException;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.u.BaseUnifier;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.nabl2.util.CapsuleUtil;

public abstract class PersistentUniDisunifier extends BaseUniDisunifier implements Serializable {

    private static final long serialVersionUID = 42L;


    private static final PersistentUniDisunifier.Immutable FINITE_EMPTY =
            new PersistentUniDisunifier.Immutable(PersistentUnifier.Immutable.of(true), CapsuleUtil.immutableSet());

    private static final PersistentUniDisunifier.Immutable INFINITE_EMPTY =
            new PersistentUniDisunifier.Immutable(PersistentUnifier.Immutable.of(false), CapsuleUtil.immutableSet());


    protected static ITermVar findRep(ITermVar var, Map.Transient<ITermVar, ITermVar> reps) {
        ITermVar rep = reps.get(var);
        if(rep == null) {
            return var;
        } else {
            rep = findRep(rep, reps);
            reps.__put(var, rep);
            return rep;
        }
    }

    ///////////////////////////////////////////
    // class Immutable
    ///////////////////////////////////////////

    public static class Immutable extends PersistentUniDisunifier implements IUniDisunifier.Immutable, Serializable {

        private static final long serialVersionUID = 42L;

        private final PersistentUnifier.Immutable unifier;
        private final Set.Immutable<Diseq> disequalities;

        Immutable(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms,
                Set.Immutable<Diseq> disequalities) {
            this(PersistentUnifier.Immutable.of(finite, reps, ranks, terms), disequalities);
        }

        Immutable(final PersistentUnifier.Immutable unifier, Set.Immutable<Diseq> disequalities) {
            this.unifier = unifier;
            this.disequalities = disequalities;
        }

        @Override public boolean isFinite() {
            return unifier.isFinite();
        }

        @Override protected IUnifier.Immutable unifier() {
            return unifier;
        }

        @Override public Set.Immutable<Diseq> disequalities() {
            return disequalities;
        }

        @Override public ITermVar findRep(ITermVar var) {
            return unifier.findRep(var);
        }

        ///////////////////////////////////////////
        // unifier functions
        ///////////////////////////////////////////

        @Override public Set.Immutable<ITermVar> domainSet() {
            return unifier.domainSet();
        }

        @Override public Set.Immutable<ITermVar> rangeSet() {
            return unifier.rangeSet();
        }

        @Override public Set.Immutable<ITermVar> varSet() {
            return unifier.varSet();
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(ITerm left, ITerm right,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException {
            final BaseUnifier.ImmutableResult<? extends IUnifier.Immutable> r;
            if((r = unifier.unify(left, right, isRigid).orElse(null)) == null) {
                return Optional.empty();
            }
            return normalizeDiseqs(r.unifier(), disequalities, isRigid).map(ud -> {
                return new ImmutableResult<>(r.result(), ud);
            });
        }

        @Override public Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(
                Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            final BaseUnifier.ImmutableResult<? extends IUnifier.Immutable> r;
            if((r = unifier.unify(equalities, isRigid).orElse(null)) == null) {
                return Optional.empty();
            }
            return normalizeDiseqs(r.unifier(), disequalities, isRigid).map(ud -> {
                return new ImmutableResult<>(r.result(), ud);
            });
        }

        @Override public Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(IUnifier other,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException {
            final BaseUnifier.ImmutableResult<? extends IUnifier.Immutable> r;
            if((r = unifier.unify(other, isRigid).orElse(null)) == null) {
                return Optional.empty();
            }
            return normalizeDiseqs(r.unifier(), disequalities, isRigid).map(ud -> {
                return new ImmutableResult<>(r.result(), ud);
            });
        }

        @Override public Optional<IUniDisunifier.Result<IUnifier.Immutable>> unify(IUniDisunifier other,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException {
            final BaseUnifier.ImmutableResult<? extends IUnifier.Immutable> r;
            if((r = unifier.unify(other, isRigid).orElse(null)) == null) {
                return Optional.empty();
            }
            return normalizeDiseqs(r.unifier(), disequalities, isRigid).map(ud -> {
                return new ImmutableResult<>(r.result(), ud);
            });
        }

        ///////////////////////////////////////////
        // diff(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUnifier.Immutable> diff(ITerm term1, ITerm term2) {
            try {
                return unify(term1, term2).map(IUniDisunifier.Result::result);
            } catch(OccursException e) {
                return Optional.empty();
            }
        }

        ///////////////////////////////////////////
        // equal(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public boolean equal(ITerm term1, ITerm term2) {
            try {
                return unify(term1, term2).map(r -> r.result().isEmpty()).orElse(false);
            } catch(OccursException e) {
                return false;
            }
        }

        ///////////////////////////////////////////
        // disunify(Set<ITermVar>, ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUniDisunifier.Result<Optional<Diseq>>> disunify(Iterable<ITermVar> universals,
                ITerm left, ITerm right, Predicate1<ITermVar> isRigid) throws RigidException {

            // create Diseq in normal from
            final IUnifier.Transient diseqs = PersistentUnifier.Immutable.of(isFinite()).melt();
            try {
                if(!diseqs.unify(left, right).isPresent()) {
                    // terms are not equal
                    return Optional.of(new ImmutableResult<>(Optional.empty(), this));
                }
            } catch(OccursException e) {
                // terms are not equal
                return Optional.of(new ImmutableResult<>(Optional.empty(), this));
            }
            final Diseq diseq = Diseq.of(universals, diseqs.freeze());
            if(diseq.isEmpty()) {
                // no disequalities left, terms are equal
                return Optional.empty();
            }

            final Function0<FreshVars> fvProvider = freshVarProvider(this);

            // check if diseq is implied -- ignore rigid vars here
            final Optional<Diseq> reducedDiseqNoRigid;
            if((reducedDiseqNoRigid = normalizeDiseq(this, fvProvider, diseq, Predicate1.never(),
                    new PersistentUnifier.Transient(unifier)).orElse(null)) != null) {
                if(reducedDiseqNoRigid.isPresent()) {
                    for(Diseq otherDiseq : disequalities) {
                        if(otherDiseq.implies(reducedDiseqNoRigid.get())) {
                            // disequality is implied, no change
                            return Optional.of(new ImmutableResult<>(Optional.empty(), this));
                        }
                    }
                } else {
                    // terms are not equal
                    return Optional.of(new ImmutableResult<>(Optional.empty(), this));
                }
            }

            // build the normalized diseq -- respecting rigid vars
            final Optional<Diseq> reducedDiseq;
            final PersistentUnifier.Transient updateableUnifier = new PersistentUnifier.Transient(unifier);
            if((reducedDiseq =
                    normalizeDiseq(unifier, fvProvider, diseq, isRigid, updateableUnifier).orElse(null)) == null) {
                // disunify failed, terms are equal
                return Optional.empty();
            }
            if(!reducedDiseq.isPresent()) {
                // terms are not equal
                return Optional.of(new ImmutableResult<>(Optional.empty(), this));
            }

            final PersistentUnifier.Immutable newUnifier = updateableUnifier.freeze();
            final Set.Immutable<Diseq> newDisequalities = disequalities.__insert(reducedDiseq.get());
            final PersistentUniDisunifier.Immutable newUniDisunifier =
                    new PersistentUniDisunifier.Immutable(newUnifier, newDisequalities);
            return Optional.of(new ImmutableResult<>(reducedDiseq, newUniDisunifier));
        }

        /**
         * Simplify disequalities for an updated unifier
         */
        private static Optional<PersistentUniDisunifier.Immutable> normalizeDiseqs(PersistentUnifier.Immutable unifier,
                Set.Immutable<Diseq> disequalities, Predicate1<ITermVar> isRigid) throws RigidException {
            final Set.Transient<Diseq> newDisequalities = CapsuleUtil.transientSet();

            final Function0<FreshVars> fvProvider = freshVarProvider(unifier);
            final PersistentUnifier.Transient updateableUnifier = new PersistentUnifier.Transient(unifier);

            // reduce all
            for(Diseq diseq : disequalities) {
                final Optional<Diseq> normalizedDiseq;
                if((normalizedDiseq =
                        normalizeDiseq(unifier, fvProvider, diseq, isRigid, updateableUnifier).orElse(null)) == null) {
                    // disunify failed
                    return Optional.empty();
                }
                if(!normalizedDiseq.isPresent()) {
                    // disequality discharged, terms are unequal
                    continue;
                }
                // not unified yet, keep
                newDisequalities.__insert(normalizedDiseq.get());
            }

            final PersistentUnifier.Immutable newUnifier = updateableUnifier.freeze();
            final Set.Immutable<Diseq> _newDisequalities = newDisequalities.freeze();
            final PersistentUniDisunifier.Immutable newUniDisunifier;
            if(newUnifier.isEmpty() && _newDisequalities.isEmpty()) {
                newUniDisunifier = of(unifier.isFinite());
            } else {
                newUniDisunifier = new PersistentUniDisunifier.Immutable(newUnifier, _newDisequalities);
            }
            return Optional.of(newUniDisunifier);
        }

        private static Function0<FreshVars> freshVarProvider(IUnifier.Immutable unifier) {
            final Ref<FreshVars> fv = new Ref<>();
            final Function0<FreshVars> fvProvider = () -> {
                FreshVars result = fv.get();
                if(result == null) {
                    result = new FreshVars(unifier.varSet());
                    fv.set(result);
                }
                return result;
            };
            return fvProvider;
        }

        /**
         * Disunify the given disequality.
         * 
         * Reduces the disequality to canonical form for the current unifier. Returns a reduced disequality, or none if
         * the disequality is satisfied.
         * 
         * @param updateableUnifier
         *            TODO
         */
        private static Optional<Optional<Diseq>> normalizeDiseq(IUnifier.Immutable unifier,
                Function0<FreshVars> fvProvider, Diseq diseq, Predicate1<ITermVar> isRigid,
                PersistentUnifier.Transient updateableUnifier) throws RigidException {
            if(!diseq.universals().isEmpty()) {
                final FreshVars fv = fvProvider.apply();
                fv.add(diseq.freeVarSet());
                diseq = diseq.rename(fv.fresh(diseq.universals()));
                fv.reset();
            }
            for(ITermVar var : diseq.freeVarSet()) {
                updateableUnifier.removeRangeVar(var, 1);
            }

            final Optional<? extends IUnifier.Result<? extends IUnifier.Immutable>> unifyResult;
            try {
                unifyResult = unifier.unify(diseq.disequalities(), isRigid);
            } catch(OccursException e) {
                // unify failed, terms are unequal
                return Optional.of(Optional.empty());
            }
            if(!unifyResult.isPresent()) {
                // unify failed, terms are unequal
                return Optional.of(Optional.empty());
            }
            // unify succeeded, terms are not unequal
            final IUnifier.Immutable diff = unifyResult.get().result();

            final Set.Immutable<ITermVar> universals = diseq.universals().stream()
                    .flatMap(v -> unifier.getVars(v).stream()).collect(CapsuleCollectors.toSet());
            final Diseq newDiseq = Diseq.of(universals, diff);

            if(newDiseq.isEmpty()) {
                // no disequalities left, terms are equal
                return Optional.empty();
            }

            for(ITermVar var : newDiseq.freeVarSet()) {
                updateableUnifier.addRangeVar(var, 1);
            }
            return Optional.of(Optional.of(newDiseq));
        }

        ///////////////////////////////////////////
        // retain(ITermVar)
        ///////////////////////////////////////////

        @Override public IUniDisunifier.Result<ISubstitution.Immutable> retain(ITermVar var) {
            return retainAll(CapsuleUtil.immutableSet(var));
        }

        @Override public IUniDisunifier.Result<ISubstitution.Immutable> retainAll(Iterable<ITermVar> vars) {
            return removeAll(Sets.difference(domainSet(), ImmutableSet.copyOf(vars)));
        }

        ///////////////////////////////////////////
        // remove(ITermVar)
        ///////////////////////////////////////////

        @Override public PersistentUniDisunifier.Result<ISubstitution.Immutable> remove(ITermVar var) {
            return removeAll(CapsuleUtil.immutableSet(var));
        }

        @Override public PersistentUniDisunifier.Result<ISubstitution.Immutable> removeAll(Iterable<ITermVar> vars) {
            final BaseUnifier.ImmutableResult<ISubstitution.Immutable> r = unifier.removeAll(vars);
            final Set.Transient<Diseq> newDisequalities = CapsuleUtil.transientSet();
            disequalities.stream().flatMap(diseq -> Streams.stream(diseq.apply(r.result())))
                    .map(diseq -> diseq.removeAll(vars)).forEach(newDisequalities::__insert);
            if(newDisequalities.stream().anyMatch(Diseq::isEmpty)) {
                // FIXME disequalities may become empty, and therefore false!
                throw new NotImplementedException("removal made disequality false, unhandled");
            }
            final PersistentUniDisunifier.Immutable ud =
                    new PersistentUniDisunifier.Immutable(r.unifier(), newDisequalities.freeze());
            return new ImmutableResult<>(r.result(), ud);
        }

        ///////////////////////////////////////////
        // rename(IRenaming)
        ///////////////////////////////////////////

        @Override public PersistentUniDisunifier.Immutable rename(IRenaming renaming) {
            if(renaming.isEmpty() || this.isEmpty()) {
                return this;
            }
            final PersistentUnifier.Immutable unifier = this.unifier.rename(renaming);
            final Set.Immutable<Diseq> disequalities =
                    this.disequalities.stream().map(diseq -> diseq.rename(renaming)).collect(CapsuleCollectors.toSet());
            return new PersistentUniDisunifier.Immutable(unifier, disequalities);
        }

        ///////////////////////////////////////////
        // construction
        ///////////////////////////////////////////

        @Override public IUniDisunifier.Transient melt() {
            return new BaseUniDisunifier.Transient(this);
        }

        public static PersistentUniDisunifier.Immutable of() {
            return FINITE_EMPTY;
        }

        public static PersistentUniDisunifier.Immutable of(boolean finite) {
            return finite ? FINITE_EMPTY : INFINITE_EMPTY;
        }

    }

}