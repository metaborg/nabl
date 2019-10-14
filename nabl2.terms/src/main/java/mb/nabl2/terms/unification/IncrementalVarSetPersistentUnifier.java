package mb.nabl2.terms.unification;

import java.io.Serializable;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSet;

public abstract class IncrementalVarSetPersistentUnifier extends BaseUnifier implements Serializable {

    private static final long serialVersionUID = 42L;

    private final static ILogger log = LoggerUtils.logger(IncrementalVarSetPersistentUnifier.class);

    protected static ITermVar findRep(ITermVar var, Map.Transient<ITermVar, ITermVar> reps,
            SetMultimap.Transient<ITermVar, ITermVar> __invReps) {
        ITermVar rep = reps.get(var);
        if(rep == null) {
            return var;
        } else {
            rep = findRep(rep, reps, __invReps);
            final ITermVar oldRep = reps.__put(var, rep);
            if(oldRep != null) {
                __invReps.__remove(oldRep, var);
            }
            __invReps.__insert(rep, var);
            return rep;
        }
    }

    ///////////////////////////////////////////
    // class Immutable
    ///////////////////////////////////////////

    public static class Immutable extends IncrementalVarSetPersistentUnifier
            implements IUnifier.Immutable, Serializable {

        private static final long serialVersionUID = 42L;

        private final boolean finite;

        private final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
        private final Map.Immutable<ITermVar, Integer> ranks;
        private final Map.Immutable<ITermVar, ITerm> terms;
        private final Ref<SetMultimap.Immutable<ITermVar, ITermVar>> __invReps;
        private final MultiSet.Immutable<ITermVar> __varSet;
        private final MultiSet.Immutable<ITermVar> __freeVarSet;
        private final MultiSet.Immutable<ITermVar> __unfreeVarSet;
        private final Set.Immutable<Diseq> disequalities;

        Immutable(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms,
                SetMultimap.Immutable<ITermVar, ITermVar> __invReps, MultiSet.Immutable<ITermVar> __varSet,
                MultiSet.Immutable<ITermVar> __freeVarSet, MultiSet.Immutable<ITermVar> __unfreeVarSet,
                Set.Immutable<Diseq> disequalities) {
            this.finite = finite;
            this.reps = new Ref<>(reps);
            this.ranks = ranks;
            this.terms = terms;
            this.__invReps = new Ref<>(__invReps);
            this.__varSet = __varSet;
            this.__freeVarSet = __freeVarSet;
            this.__unfreeVarSet = __unfreeVarSet;
            this.disequalities = disequalities;
        }

        @Override public boolean isFinite() {
            return finite;
        }

        @Override protected Map.Immutable<ITermVar, ITermVar> reps() {
            return reps.get();
        }

        @Override protected Map.Immutable<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override public Set.Immutable<Diseq> disequalities() {
            return disequalities;
        }

        @Override public ITermVar findRep(ITermVar var) {
            final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
            final SetMultimap.Transient<ITermVar, ITermVar> __invReps = this.__invReps.get().asTransient();
            final ITermVar rep = findRep(var, reps, __invReps);
            this.reps.set(reps.freeze());
            this.__invReps.set(__invReps.freeze());
            return rep;
        }

        @Override public boolean contains(ITermVar var) {
            return __varSet.contains(var);
        }

        @Override public java.util.Set<ITermVar> varSet() {
            return __varSet.elementSet();
        }

        @Override public java.util.Set<ITermVar> freeVarSet() {
            return __freeVarSet.elementSet();
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(ITerm left, ITerm right)
                throws OccursException {
            return new Unify(this, left, right).apply(true);
        }

        @Override public Optional<Result<mb.nabl2.terms.unification.IUnifier.Immutable>>
                unify(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) throws OccursException {
            return new Unify(this, equalities).apply(true);
        }

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(IUnifier other)
                throws OccursException {
            return new Unify(this, other).apply(true);
        }

        private static class Unify extends IncrementalVarSetPersistentUnifier.Transient {

            private final Deque<Map.Entry<ITerm, ITerm>> worklist = Lists.newLinkedList();
            private final List<ITermVar> result = Lists.newArrayList();

            public Unify(IncrementalVarSetPersistentUnifier.Immutable unifier, ITerm left, ITerm right) {
                super(unifier);
                worklist.push(ImmutableTuple2.of(left, right));
            }

            public Unify(IncrementalVarSetPersistentUnifier.Immutable unifier,
                    Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) {
                super(unifier);
                equalities.forEach(e -> {
                    worklist.push(Tuple2.of(e));
                });
            }

            public Unify(IncrementalVarSetPersistentUnifier.Immutable unifier, IUnifier other) {
                super(unifier);
                other.varSet().forEach(v -> {
                    worklist.push(ImmutableTuple2.of(v, other.findTerm(v)));
                });
            }

            public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> apply(boolean disunify)
                    throws OccursException {
                while(!worklist.isEmpty()) {
                    final Map.Entry<ITerm, ITerm> work = worklist.pop();
                    if(!unifyTerms(work.getKey(), work.getValue())) {
                        return Optional.empty();
                    }
                }

                final IncrementalVarSetPersistentUnifier.Immutable unifier = freeze();
                if(finite) {
                    final ImmutableSet<ITermVar> cyclicVars =
                            result.stream().filter(v -> unifier.isCyclic(v)).collect(ImmutableSet.toImmutableSet());
                    if(!cyclicVars.isEmpty()) {
                        throw new OccursException(cyclicVars);
                    }
                }
                final IUnifier.Immutable diffUnifier = diffUnifier(result);
                return (disunify ? unifier.disunifyAll() : Optional.of(unifier)).map(u -> {
                    return new BaseUnifier.ImmutableResult<>(diffUnifier, u);
                });
            }

            private boolean unifyTerms(final ITerm left, final ITerm right) {
                // @formatter:off
                return left.match(Terms.<Boolean>cases(
                    applLeft -> right.match(Terms.<Boolean>cases()
                        .appl(applRight -> {
                            return applLeft.getArity() == applRight.getArity() &&
                                    applLeft.getOp().equals(applRight.getOp()) &&
                                    unifys(applLeft.getArgs(), applRight.getArgs());
                        })
                        .var(varRight -> {
                            return unifyTerms(varRight, applLeft)  ;
                        })
                        .otherwise(t -> {
                            return false;
                        })
                    ),
                    listLeft -> right.match(Terms.<Boolean>cases()
                        .list(listRight -> {
                            return unifyLists(listLeft, listRight);
                        })
                        .var(varRight -> {
                            return unifyTerms(varRight, listLeft);
                        })
                        .otherwise(t -> {
                            return false;
                        })
                    ),
                    stringLeft -> right.match(Terms.<Boolean>cases()
                        .string(stringRight -> {
                            return stringLeft.getValue().equals(stringRight.getValue());
                        })
                        .var(varRight -> {
                            return unifyTerms(varRight, stringLeft);
                        })
                        .otherwise(t -> {
                            return false;
                        })
                    ),
                    integerLeft -> right.match(Terms.<Boolean>cases()
                        .integer(integerRight -> {
                            return integerLeft.getValue() == integerRight.getValue();
                        })
                        .var(varRight -> {
                            return unifyTerms(varRight, integerLeft);
                        })
                        .otherwise(t -> {
                            return false;
                        })
                    ),
                    blobLeft -> right.match(Terms.<Boolean>cases()
                        .blob(blobRight -> {
                            return blobLeft.getValue().equals(blobRight.getValue());
                        })
                        .var(varRight -> {
                            return unifyTerms(varRight, blobLeft);
                        })
                        .otherwise(t -> {
                            return false;
                        })
                    ),
                    varLeft -> right.match(Terms.<Boolean>cases()
                        .var(varRight -> {
                            return unifyVars(varLeft, varRight);
                        })
                        .otherwise(termRight -> {
                            return unifyVarTerm(varLeft, termRight);
                        })
                    )
                ));
                // @formatter:on
            }

            private boolean unifyLists(final IListTerm left, final IListTerm right) {
                // @formatter:off
                return left.match(ListTerms.<Boolean>cases(
                    consLeft -> right.match(ListTerms.<Boolean>cases()
                        .cons(consRight -> {
                            worklist.push(ImmutableTuple2.of(consLeft.getHead(), consRight.getHead()));
                            worklist.push(ImmutableTuple2.of(consLeft.getTail(), consRight.getTail()));
                            return true;
                        })
                        .var(varRight -> {
                            return unifyLists(varRight, consLeft);
                        })
                        .otherwise(l -> {
                            return false;
                        })
                    ),
                    nilLeft -> right.match(ListTerms.<Boolean>cases()
                        .nil(nilRight -> {
                            return true;
                        })
                        .var(varRight -> {
                            return unifyVarTerm(varRight, nilLeft)  ;
                        })
                        .otherwise(l -> {
                            return false;
                        })
                    ),
                    varLeft -> right.match(ListTerms.<Boolean>cases()
                        .var(varRight -> {
                            return unifyVars(varLeft, varRight);
                        })
                        .otherwise(termRight -> {
                            return unifyVarTerm(varLeft, termRight);
                        })
                    )
                ));
                // @formatter:on
            }

            private boolean unifyVarTerm(final ITermVar var, final ITerm term) {
                final ITermVar rep = findRep(var);
                if(term instanceof ITermVar) {
                    throw new IllegalStateException();
                }
                final ITerm repTerm = getTerm(rep); // term for the representative
                if(repTerm != null) {
                    worklist.push(ImmutableTuple2.of(repTerm, term));
                } else {
                    putTerm(rep, term);
                    result.add(rep);
                }
                return true;
            }

            private boolean unifyVars(final ITermVar left, final ITermVar right) {
                final ITermVar leftRep = findRep(left);
                final ITermVar rightRep = findRep(right);
                if(leftRep.equals(rightRep)) {
                    return true;
                }
                final int leftRank = Optional.ofNullable(ranks.__remove(leftRep)).orElse(1);
                final int rightRank = Optional.ofNullable(ranks.__remove(rightRep)).orElse(1);
                final boolean swap = leftRank > rightRank;
                final ITermVar var = swap ? rightRep : leftRep; // the eliminated variable
                final ITermVar rep = swap ? leftRep : rightRep; // the new representative
                ranks.__put(rep, leftRank + rightRank);
                putRep(var, rep);
                final ITerm varTerm = removeTerm(var); // term for the eliminated var
                if(varTerm != null) {
                    final ITerm repTerm = getTerm(rep); // term for the representative
                    if(repTerm != null) {
                        worklist.push(ImmutableTuple2.of(varTerm, repTerm));
                        // don't add to result
                    } else {
                        putTerm(rep, varTerm);
                        result.add(rep);
                    }
                } else {
                    result.add(var);
                }
                return true;
            }

            private boolean unifys(final Iterable<ITerm> lefts, final Iterable<ITerm> rights) {
                Iterator<ITerm> itLeft = lefts.iterator();
                Iterator<ITerm> itRight = rights.iterator();
                while(itLeft.hasNext()) {
                    if(!itRight.hasNext()) {
                        return false;
                    }
                    worklist.push(ImmutableTuple2.of(itLeft.next(), itRight.next()));
                }
                if(itRight.hasNext()) {
                    return false;
                }
                return true;
            }

            ///////////////////////////////////////////
            // diffUnifier(Set<ITermVar>)
            ///////////////////////////////////////////

            private IUnifier.Immutable diffUnifier(Collection<ITermVar> vars) {
                final IncrementalVarSetPersistentUnifier.Transient diff =
                        new IncrementalVarSetPersistentUnifier.Transient(finite);
                for(ITermVar var : vars) {
                    final ITermVar rep;
                    final ITerm term;
                    if((rep = getRep(var)) != null) {
                        diff.putRep(var, rep);
                    } else if((term = getTerm(var)) != null) {
                        diff.putTerm(var, term);
                    }
                }
                return diff.freeze();
            }

        }

        ///////////////////////////////////////////
        // diff(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUnifier.Immutable> diff(ITerm term1, ITerm term2) {
            try {
                return unify(term1, term2).map(Result::result);
            } catch(OccursException e) {
                return Optional.empty();
            }
        }

        ///////////////////////////////////////////
        // disunify(Set<ITermVar>, ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<Result<IUnifier.Immutable>> disunify(Iterable<ITermVar> universals, ITerm left,
                ITerm right) {
            final Optional<IUnifier.Immutable> result = disunify(new Unify(this, left, right));
            if(!result.isPresent()) {
                // disequality discharged, terms are unequal
                return Optional.of(new BaseUnifier.ImmutableResult<>(
                        IncrementalVarSetPersistentUnifier.Immutable.of(finite), this));
            }

            final IUnifier.Immutable disequality = result.get().removeAll(universals).unifier();
            if(disequality.isEmpty()) {
                // no disequalities left, terms are equal
                return Optional.empty();
            }

            final java.util.Set<ITermVar> universalVars =
                    Sets.intersection(ImmutableSet.copyOf(universals), disequality.freeVarSet());

            final IUnifier.Immutable newUnifier = new IncrementalVarSetPersistentUnifier.Immutable(finite, reps.get(),
                    ranks, terms, __invReps.get(), __varSet, __freeVarSet, __unfreeVarSet,
                    disequalities.__insert(new Diseq(universalVars, disequality.equalityMap())));
            return Optional.of(new BaseUnifier.ImmutableResult<>(disequality, newUnifier));
        }

        private Optional<IUnifier.Immutable> disunifyAll() {
            final Set.Transient<Diseq> disequalities = Set.Transient.of();
            for(Diseq diseq : this.disequalities) {
                final Optional<IUnifier.Immutable> result = disunify(new Unify(this, diseq.disequalities().entrySet()));
                if(!result.isPresent()) {
                    // disequality discharged, terms are unequal
                    disequalities.__remove(diseq);
                    continue;
                }

                final IUnifier.Immutable newDiseq = result.get().removeAll(diseq.universals()).unifier();
                if(newDiseq.isEmpty()) {
                    // no disequalities left, terms are equal
                    return Optional.empty();
                }

                final java.util.Set<ITermVar> universalVars =
                        Sets.intersection(diseq.universals(), newDiseq.freeVarSet());

                // not unified yet, keep
                disequalities.__insert(new Diseq(universalVars, newDiseq.equalityMap()));
            }
            final IUnifier.Immutable result = new IncrementalVarSetPersistentUnifier.Immutable(finite, reps.get(),
                    ranks, terms, __invReps.get(), __varSet, __freeVarSet, __unfreeVarSet, disequalities.freeze());
            return Optional.of(result);
        }

        /**
         * Disunify the given disequality.
         * 
         * Reduces the disequality to canonical form for the current unifier. Returns a reduced map of disequalities, or
         * none if the disequality is satisfied.
         */
        private Optional<IUnifier.Immutable> disunify(Unify unify) {
            final Optional<Result<IUnifier.Immutable>> unifyResult;
            try {
                // NOTE We prevent Unify from doing disunification, as this
                //      results in infinite recursion
                unifyResult = unify.apply(false);
            } catch(OccursException e) {
                // unify failed, terms are unequal
                return Optional.empty();
            }
            if(!unifyResult.isPresent()) {
                // unify failed, terms are unequal
                return Optional.empty();
            }
            // unify succeeded, terms are not unequal
            final IUnifier.Immutable diff = unifyResult.get().result();
            return Optional.of(diff);
        }

        ///////////////////////////////////////////
        // retain(ITermVar)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retain(ITermVar var) {
            return retainAll(Set.Immutable.of(var));
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retainAll(Iterable<ITermVar> vars) {
            return removeAll(Sets.difference(varSet(), ImmutableSet.copyOf(vars)));
        }

        ///////////////////////////////////////////
        // remove(ITermVar)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> remove(ITermVar var) {
            return removeAll(Iterables2.singleton(var));
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> removeAll(Iterable<ITermVar> vars) {
            return new RemoveAll(this, vars).apply();
        }

        private static class RemoveAll extends IncrementalVarSetPersistentUnifier.Transient {

            private final Set.Immutable<ITermVar> vars;

            public RemoveAll(IncrementalVarSetPersistentUnifier.Immutable unifier, Iterable<ITermVar> vars) {
                super(unifier);
                this.vars = CapsuleUtil.toSet(vars);
            }

            public IUnifier.Immutable.Result<ISubstitution.Immutable> apply() {
                // remove vars from unifier
                final ISubstitution.Immutable subst = removeAll();
                // remove disequalities
                CapsuleUtil.updateOrRemove(disequalities, diseq -> {
                    final Map.Transient<ITermVar, ITerm> newDiseq = Map.Transient.of();
                    for(Map.Entry<ITermVar, ITerm> entry : diseq.disequalities().entrySet()) {
                        ITermVar var = (ITermVar) subst.apply(entry.getKey());
                        ITerm term = subst.apply(entry.getValue());
                        if(!(vars.contains(var) || vars.contains(term))) {
                            newDiseq.__put(var, term);
                        }
                    }
                    final Set.Immutable<ITermVar> universalVars = diseq.universals().subtract(vars);
                    return newDiseq.isEmpty() ? null : new Diseq(universalVars, newDiseq.freeze());
                });
                // TODO Check if variables escaped?
                final IUnifier.Immutable newUnifier = freeze();
                return new BaseUnifier.ImmutableResult<>(subst, newUnifier);
            }

            private ISubstitution.Immutable removeAll() {
                final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
                for(ITermVar var : vars) {
                    ITermVar rep;
                    if((rep = removeRep(var)) != null) { // var |-> rep
                        subst.compose(var, rep);
                        for(ITermVar notRep : getInvReps(var)) {
                            putRep(notRep, rep);
                        }
                    } else {
                        final Collection<ITermVar> newReps = getInvReps(var);
                        if(newReps.isEmpty()) {
                            final ITerm term;
                            if((term = removeTerm(var)) != null) { // var |-> term
                                subst.compose(var, term);
                            }
                        } else { // rep |-> var
                            rep = newReps.stream().max((r1, r2) -> Integer.compare(getRank(r1), getRank(r2))).get();
                            removeRep(rep);
                            subst.compose(var, rep);
                            for(ITermVar notRep : newReps) {
                                if(!notRep.equals(rep)) {
                                    putRep(notRep, rep);
                                }
                            }
                            final ITerm term;
                            if((term = removeTerm(var)) != null) { // var |-> term
                                putTerm(rep, term);
                            }
                        }
                    }
                }
                for(Entry<ITermVar, ITerm> entry : termEntries()) {
                    final ITermVar rep = entry.getKey();
                    final ITerm term = entry.getValue();
                    putTerm(rep, subst.apply(term));
                }
                return subst.freeze();
            }

        }

        ///////////////////////////////////////////
        // construction
        ///////////////////////////////////////////

        @Override public IUnifier.Transient melt() {
            return new BaseUnifier.Transient(this);
        }

        public static IUnifier.Immutable of() {
            return of(true);
        }

        public static IUnifier.Immutable of(boolean finite) {
            return new IncrementalVarSetPersistentUnifier.Immutable(finite, Map.Immutable.of(), Map.Immutable.of(),
                    Map.Immutable.of(), SetMultimap.Immutable.of(), MultiSet.Immutable.of(), MultiSet.Immutable.of(),
                    MultiSet.Immutable.of(), Set.Immutable.of());
        }

    }

    ///////////////////////////////////////////
    // class Transient
    ///////////////////////////////////////////

    static class Transient {

        protected final boolean finite;

        private final Map.Transient<ITermVar, ITermVar> reps;
        protected final Map.Transient<ITermVar, Integer> ranks;
        private final Map.Transient<ITermVar, ITerm> terms;
        protected final Set.Transient<Diseq> disequalities;

        private final SetMultimap.Transient<ITermVar, ITermVar> __invReps;
        private final MultiSet.Transient<ITermVar> __varSet; // = reps.keys + terms.keys
        private final MultiSet.Transient<ITermVar> __freeVarSet; // = (reps.values + terms.values.vars) - varSet
        private final MultiSet.Transient<ITermVar> __unfreeVarSet; // = (reps.values + terms.values.vars) ^ varSet

        Transient(boolean finite) {
            this(finite, Map.Transient.of(), Map.Transient.of(), Map.Transient.of(), SetMultimap.Transient.of(),
                    MultiSet.Transient.of(), MultiSet.Transient.of(), MultiSet.Transient.of(), Set.Transient.of());
        }

        Transient(IncrementalVarSetPersistentUnifier.Immutable unifier) {
            this(unifier.finite, unifier.reps.get().asTransient(), unifier.ranks.asTransient(),
                    unifier.terms.asTransient(), unifier.__invReps.get().asTransient(), unifier.__varSet.melt(),
                    unifier.__freeVarSet.melt(), unifier.__unfreeVarSet.melt(), unifier.disequalities.asTransient());
        }

        Transient(boolean finite, Map.Transient<ITermVar, ITermVar> reps, Map.Transient<ITermVar, Integer> ranks,
                Map.Transient<ITermVar, ITerm> terms, Set.Transient<Diseq> disequalities) {
            this.finite = finite;
            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;
            this.disequalities = disequalities;

            this.__invReps = SetMultimap.Transient.of();
            this.__varSet = MultiSet.Transient.of();
            this.__freeVarSet = MultiSet.Transient.of();
            this.__unfreeVarSet = MultiSet.Transient.of();

            reps.forEach((v, r) -> {
                __invReps.__put(r, v);
            });

            __varSet.addAll(reps.keySet());
            __varSet.addAll(terms.keySet());

            Iterables.concat(reps.values(), terms.values()).forEach(t -> {
                t.getVars().forEach(v -> {
                    (__varSet.contains(v) ? __unfreeVarSet : __freeVarSet).add(v);
                });
            });
        }

        private Transient(boolean finite, Map.Transient<ITermVar, ITermVar> reps,
                Map.Transient<ITermVar, Integer> ranks, Map.Transient<ITermVar, ITerm> terms,
                SetMultimap.Transient<ITermVar, ITermVar> __invRep, MultiSet.Transient<ITermVar> __varSet,
                MultiSet.Transient<ITermVar> __freeVarSet, MultiSet.Transient<ITermVar> __unfreeVarSet,
                Set.Transient<Diseq> disequalities) {
            this.finite = finite;
            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;
            this.__invReps = __invRep;
            this.__varSet = __varSet;
            this.__freeVarSet = __freeVarSet;
            this.__unfreeVarSet = __unfreeVarSet;
            this.disequalities = disequalities;
        }

        protected Iterable<Entry<ITermVar, ITermVar>> repEntries() {
            return reps.entrySet();
        }

        protected Iterable<Entry<ITermVar, ITerm>> termEntries() {
            return terms.entrySet();
        }

        protected ITermVar findRep(ITermVar var) {
            return IncrementalVarSetPersistentUnifier.findRep(var, reps, __invReps);
        }

        protected ITermVar getRep(ITermVar var) {
            return reps.get(var);
        }

        protected Collection<ITermVar> getInvReps(ITermVar rep) {
            return __invReps.get(rep);
        }

        protected void putRep(ITermVar var, ITermVar rep) {
            final ITermVar oldRep = reps.__put(var, rep);
            if(oldRep != null) { // existing entry
                __removedRep(var, oldRep);
            } else { // new entry
                __addedVar(var);
            }
            __invReps.__insert(rep, var);
            (__varSet.contains(rep) ? __unfreeVarSet : __freeVarSet).add(rep);
        }

        protected ITermVar removeRep(ITermVar var) {
            final ITermVar oldRep = reps.__remove(var);
            if(oldRep != null) { // existing entry
                __removedRep(var, oldRep);
                __removedVar(var);
            }
            return oldRep;
        }

        private void __removedRep(ITermVar var, ITermVar oldRep) {
            __invReps.__remove(oldRep, var);
            (__varSet.contains(oldRep) ? __unfreeVarSet : __freeVarSet).remove(oldRep);
        }

        protected ITerm getTerm(ITermVar rep) {
            return terms.get(rep);
        }

        protected void putTerm(ITermVar rep, ITerm term) {
            ITerm oldTerm = terms.__put(rep, term);
            if(oldTerm != null) { // existing entry
                __removedTerm(oldTerm);
            } else { // new entry
                __addedVar(rep);
            }
            term.getVars().forEach(v -> {
                (__varSet.contains(v) ? __unfreeVarSet : __freeVarSet).add(v);
            });
        }

        protected ITerm removeTerm(ITermVar rep) {
            final ITerm oldTerm = terms.__remove(rep);
            if(oldTerm != null) {
                __removedTerm(oldTerm);
                __removedVar(rep);
            }
            return oldTerm;
        }

        private void __removedTerm(ITerm oldTerm) {
            oldTerm.getVars().forEach(v -> {
                (__varSet.contains(v) ? __unfreeVarSet : __freeVarSet).remove(v);
            });
        }

        private void __addedVar(ITermVar var) {
            __varSet.add(var);
            __unfreeVarSet.add(var, __freeVarSet.removeAll(var));
        }

        private void __removedVar(ITermVar var) {
            int n = __varSet.remove(var);
            if(n == 0) {
                __freeVarSet.add(var, __unfreeVarSet.removeAll(var));
            }
        }

        protected int getRank(ITermVar var) {
            return ranks.getOrDefault(var, 1);
        }

        protected IncrementalVarSetPersistentUnifier.Immutable freeze() {
            final IncrementalVarSetPersistentUnifier.Immutable unifier =
                    new IncrementalVarSetPersistentUnifier.Immutable(finite, reps.freeze(), ranks.freeze(),
                            terms.freeze(), __invReps.freeze(), __varSet.freeze(), __freeVarSet.freeze(),
                            __unfreeVarSet.freeze(), disequalities.freeze());

            /*
            {
                unifier.reps.get().forEach((v, r) -> {
                    final SetMultimap.Immutable<ITermVar, ITermVar> invReps = unifier.__invReps.get();
                    if(!invReps.containsEntry(r, v)) {
                        throw new AssertionError("Missing inverse of " + v + " |-> " + r);
                    }
                });
            }

            {
                MultiSet.Immutable<ITermVar> newVarSet = unifier.__varSet;
                final java.util.Set<ITermVar> varSet = unifier.varSet();
                SetView<ITermVar> varSetDiff = Sets.symmetricDifference(varSet, newVarSet.elementSet());
                if(!varSetDiff.isEmpty()) {
                    log.warn("org varSet {}", varSet);
                    log.warn("new varSet {}", newVarSet);
                    throw new AssertionError("Expected varSet " + varSet + ", got " + newVarSet);
                }
            }

            {
                MultiSet.Immutable<ITermVar> newFreeVarSet = unifier.__freeVarSet;
                final java.util.Set<ITermVar> freeVarSet = unifier.freeVarSet();
                SetView<ITermVar> freeVarSetDiff = Sets.symmetricDifference(freeVarSet, newFreeVarSet.elementSet());
                if(!freeVarSetDiff.isEmpty()) {
                    log.warn("org freeVarSet {}", freeVarSet);
                    log.warn("new freeVarSet {}", newFreeVarSet);
                    throw new AssertionError("Expected freeVars " + freeVarSet + ", got " + newFreeVarSet);
                }
            }
            */

            return unifier;
        }

    }

}