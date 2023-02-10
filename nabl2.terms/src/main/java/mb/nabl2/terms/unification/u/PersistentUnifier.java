package mb.nabl2.terms.unification.u;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.BagMap;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.IReplacement;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;

public abstract class PersistentUnifier extends BaseUnifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;


    private static final PersistentUnifier.Immutable FINITE_EMPTY = new PersistentUnifier.Immutable(true,
            Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of(), MultiSet.Immutable.of(),
            CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet());

    private static final PersistentUnifier.Immutable INFINITE_EMPTY = new PersistentUnifier.Immutable(false,
            Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of(), MultiSet.Immutable.of(),
            CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet());


    ///////////////////////////////////////////
    // class Immutable
    ///////////////////////////////////////////

    public static class Immutable extends PersistentUnifier implements IUnifier.Immutable, Serializable {

        private static final long serialVersionUID = 42L;

        private final boolean finite;

        private final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
        private final Map.Immutable<ITermVar, Integer> ranks;
        private final Map.Immutable<ITermVar, ITerm> terms;

        private final Ref<MultiSet.Immutable<ITermVar>> repAndTermVarsCache;
        private final Set.Immutable<ITermVar> domainSetCache;
        private final Set.Immutable<ITermVar> rangeSetCache;
        private final Set.Immutable<ITermVar> varSetCache;

        // FIXME Should be `package`, but is `public` for constructor in PersistentUniDisunifier
        public Immutable(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms,
                MultiSet.Immutable<ITermVar> repAndTermVarsCache, Set.Immutable<ITermVar> domainSetCache,
                Set.Immutable<ITermVar> rangeSetCache, Set.Immutable<ITermVar> varSetCache) {
            this.finite = finite;

            this.reps = new Ref<>(reps);
            this.ranks = ranks;
            this.terms = terms;

            this.repAndTermVarsCache = new Ref<>(repAndTermVarsCache);
            this.domainSetCache = domainSetCache;
            this.rangeSetCache = rangeSetCache;
            this.varSetCache = varSetCache;
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

        @Override public ITermVar findRep(ITermVar var) {
            final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
            final MultiSet.Transient<ITermVar> repAndTermVarsCache = this.repAndTermVarsCache.get().melt();
            final ITermVar rep = findRep(var, reps, repAndTermVarsCache);
            this.reps.set(reps.freeze());
            this.repAndTermVarsCache.set(repAndTermVarsCache.freeze());
            return rep;
        }

        ///////////////////////////////////////////
        // unifier functions
        ///////////////////////////////////////////

        @Override public Set.Immutable<ITermVar> domainSet() {
            return domainSetCache;
        }

        @Override public Set.Immutable<ITermVar> rangeSet() {
            return rangeSetCache;
        }

        @Override public Set.Immutable<ITermVar> varSet() {
            return varSetCache;
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<ImmutableResult<IUnifier.Immutable>> unify(ITerm left, ITerm right,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException {
            return new Unify(this, left, right, isRigid).apply();
        }

        @Override public Optional<ImmutableResult<IUnifier.Immutable>> unify(
                Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            return new Unify(this, equalities, isRigid).apply();
        }

        @Override public Optional<ImmutableResult<IUnifier.Immutable>> unify(IUnifier other,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidException {
            return new Unify(this, other, isRigid).apply();
        }

        private static class Unify extends PersistentUnifier.Transient {

            private final Predicate1<ITermVar> isRigid;
            private final Deque<Map.Entry<ITerm, ITerm>> worklist = new ArrayDeque<>();
            private final List<ITermVar> result = new ArrayList<>();

            public Unify(PersistentUnifier.Immutable unifier, ITerm left, ITerm right, Predicate1<ITermVar> isRigid) {
                super(unifier);
                this.isRigid = isRigid;
                worklist.push(Tuple2.of(left, right));
            }

            public Unify(PersistentUnifier.Immutable unifier,
                    Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities,
                    Predicate1<ITermVar> isRigid) {
                super(unifier);
                this.isRigid = isRigid;
                for(Entry<? extends ITerm, ? extends ITerm> e : equalities) {
                    worklist.push(Tuple2.of(e));
                }
            }

            public Unify(PersistentUnifier.Immutable unifier, IUnifier other, Predicate1<ITermVar> isRigid) {
                super(unifier);
                this.isRigid = isRigid;
                for(ITermVar v : other.domainSet()) {
                    worklist.push(Tuple2.of(v, other.findTerm(v)));
                }
            }

            public Optional<ImmutableResult<IUnifier.Immutable>> apply() throws OccursException, RigidException {
                while(!worklist.isEmpty()) {
                    final Map.Entry<ITerm, ITerm> work = worklist.pop();
                    if(!unifyTerms(work.getKey(), work.getValue())) {
                        return Optional.empty();
                    }
                }

                final PersistentUnifier.Immutable unifier = freeze();
                if(finite) {
                    occursCheck(unifier);
                }
                final IUnifier.Immutable diffUnifier = diffUnifier(result);
                return Optional.of(new ImmutableResult<>(diffUnifier, unifier));
            }

            private void occursCheck(final PersistentUnifier.Immutable unifier) throws OccursException {
                final Set.Transient<ITermVar> _cyclicVars = Set.Transient.of();
                for(ITermVar var : result) {
                    if(unifier.isCyclic(var)) {
                        _cyclicVars.__insert(var);
                    }
                }
                final Set.Immutable<ITermVar> cyclicVars = _cyclicVars.freeze();
                if(!cyclicVars.isEmpty()) {
                    throw new OccursException(cyclicVars);
                }
            }

            private boolean unifyTerms(final ITerm left, final ITerm right) throws RigidException {
                // @formatter:off
                return left.matchOrThrow(Terms.<Boolean,RigidException>checkedCases(
                    applLeft -> right.matchOrThrow(Terms.<Boolean,RigidException>checkedCases()
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
                    listLeft -> right.matchOrThrow(Terms.<Boolean,RigidException>checkedCases()
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
                    stringLeft -> right.matchOrThrow(Terms.<Boolean,RigidException>checkedCases()
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
                    integerLeft -> right.matchOrThrow(Terms.<Boolean,RigidException>checkedCases()
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
                    blobLeft -> right.matchOrThrow(Terms.<Boolean,RigidException>checkedCases()
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
                    varLeft -> right.matchOrThrow(Terms.<Boolean,RigidException>checkedCases()
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

            private boolean unifyLists(final IListTerm left, final IListTerm right) throws RigidException {
                // @formatter:off
                return left.matchOrThrow(ListTerms.<Boolean,RigidException>checkedCases(
                    consLeft -> right.matchOrThrow(ListTerms.<Boolean,RigidException>checkedCases()
                        .cons(consRight -> {
                            worklist.push(Tuple2.of(consLeft.getHead(), consRight.getHead()));
                            worklist.push(Tuple2.of(consLeft.getTail(), consRight.getTail()));
                            return true;
                        })
                        .var(varRight -> {
                            return unifyLists(varRight, consLeft);
                        })
                        .otherwise(l -> {
                            return false;
                        })
                    ),
                    nilLeft -> right.matchOrThrow(ListTerms.<Boolean,RigidException>checkedCases()
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
                    varLeft -> right.matchOrThrow(ListTerms.<Boolean,RigidException>checkedCases()
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

            private boolean unifyVarTerm(final ITermVar var, final ITerm term) throws RigidException {
                final ITermVar rep = findRep(var);
                if(term instanceof ITermVar) {
                    throw new IllegalStateException();
                }
                final ITerm repTerm = getTerm(rep); // term for the representative
                if(repTerm != null) {
                    worklist.push(Tuple2.of(repTerm, term));
                } else {
                    if(isRigid.test(rep)) {
                        throw new RigidException(rep);
                    }
                    putTerm(rep, term);
                    result.add(rep);
                }
                return true;
            }

            private boolean unifyVars(final ITermVar left, final ITermVar right) throws RigidException {
                final ITermVar leftRep = findRep(left);
                final ITermVar rightRep = findRep(right);
                if(leftRep.equals(rightRep)) {
                    return true;
                }
                final boolean leftRigid = isRigid.test(leftRep);
                final boolean rightRigid = isRigid.test(rightRep);
                final ITerm leftTerm = getTerm(leftRep);
                final ITerm rightTerm = getTerm(rightRep);
                final int leftRank = Optional.ofNullable(ranks.__remove(leftRep)).orElse(1);
                final int rightRank = Optional.ofNullable(ranks.__remove(rightRep)).orElse(1);
                final ITermVar var; // the eliminated variable
                final ITermVar rep; // the new representative
                if(leftRigid && rightRigid && leftTerm == null && rightTerm == null) {
                    throw new RigidException(leftRep, rightRep);
                } else if(leftRigid && rightRigid && leftTerm == null) {
                    throw new RigidException(leftRep);
                } else if(leftRigid && rightRigid && rightTerm == null) {
                    throw new RigidException(rightRep);
                } else if(leftRigid && leftTerm == null) {
                    var = rightRep;
                    rep = leftRep;
                } else if(rightRigid && rightTerm == null) {
                    var = leftRep;
                    rep = rightRep;
                } else {
                    final boolean swap = leftRank > rightRank;
                    var = swap ? rightRep : leftRep; // the eliminated variable
                    rep = swap ? leftRep : rightRep; // the new representative
                }
                ranks.__put(rep, leftRank + rightRank);
                final ITerm varTerm = removeTerm(var); // term for the eliminated var
                putRep(var, rep);
                if(varTerm != null) {
                    final ITerm repTerm = getTerm(rep); // term for the representative
                    if(repTerm != null) {
                        worklist.push(Tuple2.of(varTerm, repTerm));
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
                    worklist.push(Tuple2.of(itLeft.next(), itRight.next()));
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
                final PersistentUnifier.Transient diff = new PersistentUnifier.Transient(finite);
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
        // retain(ITermVar)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retain(ITermVar var) {
            return retainAll(CapsuleUtil.immutableSet(var));
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retainAll(Set.Immutable<ITermVar> vars) {
            return removeAll(Set.Immutable.subtract(domainSet(), CapsuleUtil.toSet(vars)));
        }

        ///////////////////////////////////////////
        // remove(ITermVar)
        ///////////////////////////////////////////

        @Override public ImmutableResult<ISubstitution.Immutable> remove(ITermVar var) {
            return removeAll(CapsuleUtil.immutableSet(var));
        }

        @Override public ImmutableResult<ISubstitution.Immutable> removeAll(Set.Immutable<ITermVar> vars) {
            return new RemoveAll(this, vars).apply();
        }

        private static class RemoveAll extends PersistentUnifier.Transient {

            private final Set.Immutable<ITermVar> vars;

            public RemoveAll(PersistentUnifier.Immutable unifier, Set.Immutable<ITermVar> vars) {
                super(unifier);
                this.vars = vars;
            }

            public ImmutableResult<ISubstitution.Immutable> apply() {
                // remove vars from unifier
                final ISubstitution.Immutable subst = removeAll();
                final PersistentUnifier.Immutable newUnifier = freeze();
                return new BaseUnifier.ImmutableResult<>(subst, newUnifier);
            }

            private ISubstitution.Immutable removeAll() {
                final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
                if(vars.isEmpty()) {
                    return subst.freeze();
                }
                final BagMap.Transient<ITermVar, ITermVar> invReps = getInvReps(); // rep |-> [var]
                for(ITermVar var : vars) {
                    ITermVar rep;
                    if((rep = removeRep(var)) != null) { // Case 1. Var _has_ a rep; var |-> rep
                        invReps.remove(rep, var);
                        subst.compose(var, rep);
                        for(ITermVar notRep : invReps.get(var)) {
                            putRep(notRep, rep);
                            invReps.put(rep, notRep);
                        }
                    } else {
                        final Collection<ITermVar> newReps = invReps.removeAll(var);
                        if(!newReps.isEmpty()) { // Case 2. Var _is_ a rep; rep |-> var
                            rep = newReps.stream().max((r1, r2) -> Integer.compare(getRank(r1), getRank(r2))).get();
                            removeRep(rep);
                            invReps.remove(rep, var);
                            subst.compose(var, rep);
                            for(ITermVar notRep : newReps) {
                                if(!notRep.equals(rep)) {
                                    putRep(notRep, rep);
                                    invReps.put(rep, notRep);
                                }
                            }
                            final ITerm term;
                            if((term = removeTerm(var)) != null) { // var |-> term
                                putTerm(rep, term);
                            }
                        } else { // Case 3. Var neither _is_ nor _has_ a rep
                            final ITerm term;
                            if((term = removeTerm(var)) != null) { // var |-> term
                                subst.compose(var, term);
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
        // rename(IRenaming)
        ///////////////////////////////////////////

        @Override public PersistentUnifier.Immutable rename(IRenaming renaming) {
            if(renaming.isEmpty() || this.isEmpty()) {
                return this;
            }
            final Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
            for(Entry<ITermVar, ITermVar> e : this.reps.get().entrySet()) {
                reps.__put(renaming.rename(e.getKey()), renaming.rename(e.getValue()));
            }
            final Map.Transient<ITermVar, Integer> ranks = Map.Transient.of();
            for(Entry<ITermVar, Integer> e : this.ranks.entrySet()) {
                ranks.__put(renaming.rename(e.getKey()), e.getValue());
            }
            final Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
            for(Entry<ITermVar, ITerm> e : this.terms.entrySet()) {
                terms.__put(renaming.rename(e.getKey()), renaming.apply(e.getValue()));
            }
            final MultiSet.Transient<ITermVar> repAndTermVarsCache = MultiSet.Transient.of();
            for(Entry<ITermVar, Integer> e : this.repAndTermVarsCache.get().entrySet()) {
                repAndTermVarsCache.add(renaming.rename(e.getKey()), e.getValue());
            }
            final Set.Transient<ITermVar> domainSetCache = CapsuleUtil.transientSet();
            for(ITermVar var : this.domainSetCache) {
                domainSetCache.__insert(renaming.rename(var));
            }
            final Set.Transient<ITermVar> rangeSetCache = CapsuleUtil.transientSet();
            for(ITermVar var : this.rangeSetCache) {
                rangeSetCache.__insert(renaming.rename(var));
            }
            final Set.Transient<ITermVar> varSetCache = CapsuleUtil.transientSet();
            for(ITermVar var : this.varSetCache) {
                varSetCache.__insert(renaming.rename(var));
            }
            return new PersistentUnifier.Immutable(finite, reps.freeze(), ranks.freeze(), terms.freeze(),
                    repAndTermVarsCache.freeze(), domainSetCache.freeze(), rangeSetCache.freeze(),
                    varSetCache.freeze());
        }

        ///////////////////////////////////////////
        // replace(IReplacement)
        ///////////////////////////////////////////

        @Override public PersistentUnifier.Immutable replace(IReplacement replacement) {
            if(replacement.isEmpty() || this.isEmpty()) {
                return this;
            }
            final Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
            for(Entry<ITermVar, ITerm> e : this.terms.entrySet()) {
                terms.__put(e.getKey(), replacement.apply(e.getValue()));
            }
            return new PersistentUnifier.Immutable(finite, reps.get(), ranks, terms.freeze(),
                    repAndTermVarsCache.get(), domainSetCache, rangeSetCache,
                    varSetCache);
        }

        ///////////////////////////////////////////
        // construction
        ///////////////////////////////////////////

        @Override public IUnifier.Transient melt() {
            return new BaseUnifier.Transient(this);
        }

        public static PersistentUnifier.Immutable of() {
            return FINITE_EMPTY;
        }

        public static PersistentUnifier.Immutable of(boolean finite) {
            return finite ? FINITE_EMPTY : INFINITE_EMPTY;
        }

        public static PersistentUnifier.Immutable of(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms) {
            if(reps.isEmpty() && ranks.isEmpty() && terms.isEmpty()) {
                return of(finite);
            }
            final MultiSet.Transient<ITermVar> repAndTermVarsCache = MultiSet.Transient.of();
            final Set.Transient<ITermVar> domainSetCache = CapsuleUtil.transientSet();
            final Set.Transient<ITermVar> varSetCache = CapsuleUtil.transientSet();
            for(Entry<ITermVar, ITermVar> e : reps.entrySet()) {
                domainSetCache.__insert(e.getKey());
                varSetCache.__insert(e.getKey());
                repAndTermVarsCache.add(e.getValue());
                varSetCache.__insert(e.getValue());
            }
            for(Entry<ITermVar, ITerm> e : terms.entrySet()) {
                domainSetCache.__insert(e.getKey());
                varSetCache.__insert(e.getKey());
                for(ITermVar tv : e.getValue().getVars()) {
                    repAndTermVarsCache.add(tv);
                    varSetCache.__insert(tv);
                }
            }

            final Set.Transient<ITermVar> rangeSetCache = CapsuleUtil.transientSet();
            for(ITermVar var : repAndTermVarsCache.elementSet()) {
                if(!domainSetCache.contains(var)) {
                    rangeSetCache.__insert(var);
                }
            }

            return new PersistentUnifier.Immutable(finite, reps, ranks, terms, repAndTermVarsCache.freeze(),
                    domainSetCache.freeze(), rangeSetCache.freeze(), varSetCache.freeze());
        }

    }

    ///////////////////////////////////////////
    // class Transient
    ///////////////////////////////////////////

    // FIXME public because of use in PersistentUniDisunifier
    public static class Transient {

        protected final boolean finite;

        private final Map.Transient<ITermVar, ITermVar> reps;
        protected final Map.Transient<ITermVar, Integer> ranks;
        private final Map.Transient<ITermVar, ITerm> terms;

        private final MultiSet.Transient<ITermVar> repAndTermVarsCache;
        private final Set.Transient<ITermVar> domainSetCache;
        private final Set.Transient<ITermVar> rangeSetCache;
        private final Set.Transient<ITermVar> varSetCache;

        Transient(boolean finite) {
            this(finite, Map.Transient.of(), Map.Transient.of(), Map.Transient.of(), MultiSet.Transient.of(),
                    CapsuleUtil.transientSet(), CapsuleUtil.transientSet(), CapsuleUtil.transientSet());
        }

        public Transient(PersistentUnifier.Immutable unifier) {
            this(unifier.finite, unifier.reps.get().asTransient(), unifier.ranks.asTransient(),
                    unifier.terms.asTransient(), unifier.repAndTermVarsCache.get().melt(),
                    unifier.domainSetCache.asTransient(), unifier.rangeSetCache.asTransient(),
                    unifier.varSetCache.asTransient());
        }

        Transient(boolean finite, Map.Transient<ITermVar, ITermVar> reps, Map.Transient<ITermVar, Integer> ranks,
                Map.Transient<ITermVar, ITerm> terms, MultiSet.Transient<ITermVar> repAndTermVarsCache,
                Set.Transient<ITermVar> domainSetCache, Set.Transient<ITermVar> rangeSetCache,
                Set.Transient<ITermVar> varSetCache) {
            this.finite = finite;

            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;

            this.repAndTermVarsCache = repAndTermVarsCache;
            this.domainSetCache = domainSetCache;
            this.rangeSetCache = rangeSetCache;
            this.varSetCache = varSetCache;
        }

        protected Iterable<Entry<ITermVar, ITermVar>> repEntries() {
            return reps.entrySet();
        }

        protected Iterable<Entry<ITermVar, ITerm>> termEntries() {
            return terms.entrySet();
        }


        protected ITermVar findRep(ITermVar var) {
            return PersistentUnifier.findRep(var, reps, repAndTermVarsCache);
        }

        protected ITermVar getRep(ITermVar var) {
            return reps.get(var);
        }

        protected BagMap.Transient<ITermVar, ITermVar> getInvReps() {
            final BagMap.Transient<ITermVar, ITermVar> invReps = BagMap.Transient.withSizeEstimate(reps.size());
            for(Entry<ITermVar, ITermVar> e : reps.entrySet()) {
                invReps.put(e.getValue(), e.getKey());
            }
            return invReps;
        }

        protected void putRep(ITermVar var, ITermVar rep) {
            if(terms.containsKey(var)) {
                throw new IllegalStateException();
            }
            final ITermVar oldRep = reps.__put(var, rep);
            if(oldRep == null) {
                addDomainVar(var);
            } else {
                removeRangeVar(oldRep, 1);
            }
            addRangeVar(rep, 1);
        }

        protected ITermVar removeRep(ITermVar var) {
            final ITermVar rep = reps.__remove(var);
            if(rep != null) {
                removeDomainVar(var);
                removeRangeVar(rep, 1);
            }
            return rep;
        }


        protected ITerm getTerm(ITermVar rep) {
            return terms.get(rep);
        }

        protected void putTerm(ITermVar rep, ITerm term) {
            if(reps.containsKey(rep)) {
                throw new IllegalStateException();
            }
            final ITerm oldTerm = terms.__put(rep, term);
            if(oldTerm == null) {
                addDomainVar(rep);
            } else {
                for(ITermVar var : oldTerm.getVars()) {
                    removeRangeVar(var, 1);
                }
            }
            for(ITermVar var : term.getVars()) {
                addRangeVar(var, 1);
            }
        }

        protected ITerm removeTerm(ITermVar rep) {
            final ITerm term = terms.__remove(rep);
            if(term != null) {
                removeDomainVar(rep);
                for(ITermVar var : term.getVars()) {
                    removeRangeVar(var, 1);
                }
            }
            return term;
        }


        private void addDomainVar(ITermVar var) {
            if(domainSetCache.contains(var)) {
                throw new IllegalStateException();
            }
            domainSetCache.__insert(var);
            if(repAndTermVarsCache.count(var) > 0) { // pre-existing var
                rangeSetCache.__remove(var);
            } else { // added var
                varSetCache.__insert(var);
            }
        }

        private void removeDomainVar(ITermVar var) {
            if(!domainSetCache.contains(var)) {
                throw new IllegalStateException();
            }
            domainSetCache.__remove(var);
            if(repAndTermVarsCache.count(var) > 0) { // still existing var
                rangeSetCache.__insert(var);
            } else { // removed var
                varSetCache.__remove(var);
            }
        }

        public void addRangeVar(ITermVar var, int k) {
            if(k <= 0) {
                throw new IllegalStateException();
            }
            final int n = repAndTermVarsCache.add(var, k);
            if(n == 0) { // added var
                if(!domainSetCache.contains(var)) {
                    rangeSetCache.__insert(var);
                    varSetCache.__insert(var);
                }
            }
        }

        public void removeRangeVar(ITermVar var, int k) {
            if(k <= 0) {
                throw new IllegalStateException();
            }
            final int n = repAndTermVarsCache.remove(var, k);
            if(n < k) {
                throw new IllegalStateException();
            }
            if(n == k) { // removed var
                if(!domainSetCache.contains(var)) {
                    rangeSetCache.__remove(var);
                    varSetCache.__remove(var);
                }
            }
        }


        protected int getRank(ITermVar var) {
            return ranks.getOrDefault(var, 1);
        }


        public PersistentUnifier.Immutable freeze() {
            // We also check if the variable caches are empty, since they are not
            // only derived from reps/ranks/terms, but also updated based on
            // disequalities in the UniDisunifier implementation.
            if(reps.isEmpty() && ranks.isEmpty() && terms.isEmpty() && repAndTermVarsCache.isEmpty()
                    && domainSetCache.isEmpty() && rangeSetCache.isEmpty() && varSetCache.isEmpty()) {
                return Immutable.of(finite);
            } else {
                final PersistentUnifier.Immutable unifier = new PersistentUnifier.Immutable(finite, reps.freeze(),
                        ranks.freeze(), terms.freeze(), repAndTermVarsCache.freeze(), domainSetCache.freeze(),
                        rangeSetCache.freeze(), varSetCache.freeze());
                return unifier;
            }
        }

    }

    ///////////////////////////////////////////
    // utils
    ///////////////////////////////////////////

    protected static ITermVar findRep(ITermVar var, Map.Transient<ITermVar, ITermVar> reps,
            MultiSet.Transient<ITermVar> repAndTermVarsCache) {
        final ITermVar rep = reps.get(var);
        if(rep == null) {
            return var;
        } else {
            final ITermVar rep2 = findRep(rep, reps, repAndTermVarsCache);
            if(!rep2.equals(rep)) {
                reps.__put(var, rep2);
                repAndTermVarsCache.remove(rep);
                repAndTermVarsCache.add(rep2);
            }
            return rep2;
        }
    }

}