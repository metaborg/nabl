package mb.nabl2.terms.unification;

import java.io.Serializable;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.MaybeNotInstantiatedBool;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public abstract class PersistentUnifier extends BaseUnifier implements Serializable {

    private static final long serialVersionUID = 42L;

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

    public static class Immutable extends PersistentUnifier implements IUnifier.Immutable, Serializable {

        private static final long serialVersionUID = 42L;

        private final boolean finite;

        private final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
        private final Map.Immutable<ITermVar, Integer> ranks;
        private final Map.Immutable<ITermVar, ITerm> terms;
        private final Set.Immutable<Map.Immutable<ITermVar, ITerm>> disequalities;

        Immutable(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms,
                Set.Immutable<Map.Immutable<ITermVar, ITerm>> disequalities) {
            this.finite = finite;
            this.reps = new Ref<>(reps);
            this.ranks = ranks;
            this.terms = terms;
            this.disequalities = disequalities;
        }

        @Override public boolean isFinite() {
            return finite;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps.get();
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override protected Set.Immutable<Map.Immutable<ITermVar, ITerm>> disequalities() {
            return disequalities;
        }

        @Override public ITermVar findRep(ITermVar var) {
            final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
            final ITermVar rep = findRep(var, reps);
            this.reps.set(reps.freeze());
            return rep;
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(ITerm left, ITerm right)
                throws OccursException {
            return new Unify(left, right).apply(true);
        }

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(IUnifier other)
                throws OccursException {
            return new Unify(other.asEqualityMap()).apply(true);
        }

        private class Unify extends Transient {

            private static final long serialVersionUID = 1L;

            private final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            private final List<ITermVar> result = Lists.newArrayList();

            public Unify(ITerm left, ITerm right) {
                worklist.push(ImmutableTuple2.of(left, right));
            }

            public Unify(java.util.Map<ITermVar, ITerm> other) {
                other.entrySet().forEach(e -> {
                    worklist.push(Tuple2.of(e));
                });
            }

            public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> apply(boolean disunify)
                    throws OccursException {
                while(!worklist.isEmpty()) {
                    final Tuple2<ITerm, ITerm> work = worklist.pop();
                    if(!unifyTerms(work._1(), work._2())) {
                        return Optional.empty();
                    }
                }

                if(isFinite()) {
                    final ImmutableSet<ITermVar> cyclicVars =
                            result.stream().filter(v -> isCyclic(v)).collect(ImmutableSet.toImmutableSet());
                    if(!cyclicVars.isEmpty()) {
                        throw new OccursException(cyclicVars);
                    }
                }
                final PersistentUnifier.Immutable unifier = new PersistentUnifier.Immutable(finite, reps.freeze(),
                        ranks.freeze(), terms.freeze(), disequalities);
                final IUnifier.Immutable diffUnifier = diffUnifier(result);
                return (disunify ? unifier.disunifyAll() : Optional.of(unifier)).map(u -> {
                    return new BaseUnifier.ImmutableResult<>(diffUnifier, u);
                });
            }

            private boolean unifyTerms(final ITerm _left, final ITerm _right) {
                final ITerm left = findTerm(_left);
                final ITerm right = findTerm(_right);
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

            private boolean unifyLists(final IListTerm _left, final IListTerm _right) {
                final IListTerm left = (IListTerm) findTerm(_left);
                final IListTerm right = (IListTerm) findTerm(_right);
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
                assert !(term instanceof ITermVar);
                if(terms.containsKey(rep)) {
                    worklist.push(ImmutableTuple2.of(terms.get(rep), term));
                } else {
                    terms.__put(rep, term);
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
                final int leftRank = ranks.getOrDefault(leftRep, 1);
                final int rightRank = ranks.getOrDefault(rightRep, 1);
                final boolean swap = leftRank > rightRank;
                final ITermVar var = swap ? rightRep : leftRep; // the eliminated variable
                final ITermVar with = swap ? leftRep : rightRep; // the new representative
                ranks.__put(with, leftRank + rightRank);
                reps.__put(var, with);
                result.add(var);
                final ITerm term = terms.__remove(var); // term for the eliminated var
                if(term != null) {
                    worklist.push(ImmutableTuple2.of(term, terms.getOrDefault(with, with)));
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
                final Map.Transient<ITermVar, ITermVar> diffReps = Map.Transient.of();
                final Map.Transient<ITermVar, ITerm> diffTerms = Map.Transient.of();
                for(ITermVar var : vars) {
                    if(reps.containsKey(var)) {
                        diffReps.__put(var, reps.get(var));
                    } else if(terms.containsKey(var)) {
                        diffTerms.__put(var, terms.get(var));
                    }
                }
                return new PersistentUnifier.Immutable(finite, diffReps.freeze(), Map.Immutable.of(),
                        diffTerms.freeze(), Set.Immutable.of());
            }

        }

        ///////////////////////////////////////////
        // disunify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUnifier.Immutable> disunify(ITerm left, ITerm right) {
            final Optional<Result<IUnifier.Immutable>> unifyResult;
            try {
                unifyResult = new Unify(left, right).apply(false);
            } catch(OccursException e) {
                // unify failed, terms are unequal
                return Optional.of(this);
            }
            if(!unifyResult.isPresent()) {
                // unify failed, terms are unequal
                return Optional.of(this);
            }
            final IUnifier.Immutable diff = unifyResult.get().result();
            if(diff.isEmpty()) {
                // already unified, terms are equal, error
                return Optional.empty();
            }
            final Map.Immutable<ITermVar, ITerm> disequality = CapsuleUtil.toMap(diff.asEqualityMap());
            final IUnifier.Immutable result = new PersistentUnifier.Immutable(finite, reps.get(), ranks, terms,
                    disequalities.__insert(disequality));
            return Optional.of(result);
        }

        private Optional<IUnifier.Immutable> disunifyAll() {
            final Set.Transient<Map.Immutable<ITermVar, ITerm>> newDisequalities = Set.Transient.of();
            for(Map.Immutable<ITermVar, ITerm> disequality : disequalities) {
                try {
                    // NOTE We prevent Unify from doing disunification, as this
                    //      results in infinite recursion
                    final Optional<Result<IUnifier.Immutable>> unifyResult = new Unify(disequality).apply(false);
                    if(unifyResult.isPresent()) {
                        // unify succeeded, terms are not unequal
                        final IUnifier.Immutable diff = unifyResult.get().result();
                        if(diff.isEmpty()) {
                            // already unified, terms are equal, error
                            return Optional.empty();
                        } else {
                            // not unified yet, keep
                            final Map.Immutable<ITermVar, ITerm> newDisequality =
                                    CapsuleUtil.toMap(diff.asEqualityMap());
                            newDisequalities.__insert(newDisequality);
                        }
                    }
                } catch(OccursException e) {
                    // unify failed, terms are unequal
                }
            }
            return Optional
                    .of(new PersistentUnifier.Immutable(finite, reps.get(), ranks, terms, newDisequalities.freeze()));
        }

        ///////////////////////////////////////////
        // retain(ITermVar)
        ///////////////////////////////////////////

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retain(ITermVar var) {
            return retainAll(Iterables2.singleton(var));
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
            return new RemoveAll(vars).apply();
        }

        private class RemoveAll extends Transient {

            private static final long serialVersionUID = 1L;

            private final List<ITermVar> vars;

            public RemoveAll(Iterable<ITermVar> vars) {
                this.vars = ImmutableList.copyOf(vars);
            }

            public IUnifier.Immutable.Result<ISubstitution.Immutable> apply() {
                final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
                for(ITermVar var : vars) {
                    subst.compose(remove(var));
                }
                final IUnifier.Immutable unifier = new PersistentUnifier.Immutable(finite, reps.freeze(),
                        ranks.freeze(), terms.freeze(), disequalities);
                return new BaseUnifier.ImmutableResult<>(subst.freeze(), unifier);
            }

            private ISubstitution.Immutable remove(ITermVar var) {
                final ISubstitution.Immutable subst;
                if(reps.containsKey(var)) { // var |-> rep
                    final ITermVar rep = reps.__remove(var);
                    CapsuleUtil.replace(reps, (v, r) -> r.equals(var) ? rep : r);
                    subst = PersistentSubstitution.Immutable.of(var, rep);
                } else {
                    final Optional<ITermVar> maybeNewRep = reps.entrySet().stream()
                            .filter(e -> e.getValue().equals(var)).map(e -> e.getKey()).findAny();
                    if(maybeNewRep.isPresent()) { // newRep |-> var
                        final ITermVar newRep = maybeNewRep.get();
                        reps.__remove(newRep);
                        CapsuleUtil.replace(reps, (v, r) -> r.equals(var) ? newRep : r);
                        if(terms.containsKey(var)) { // var -> term
                            final ITerm term = terms.__remove(var);
                            terms.__put(newRep, term);
                        }
                        subst = PersistentSubstitution.Immutable.of(var, newRep);
                    } else {
                        if(terms.containsKey(var)) { // var -> term
                            final ITerm term = terms.__remove(var);
                            subst = PersistentSubstitution.Immutable.of(var, term);
                        } else { // var free -- cannot eliminate
                            subst = PersistentSubstitution.Immutable.of();
                        }
                    }
                }
                CapsuleUtil.replace(terms, (v, t) -> subst.apply(t));
                return subst;
            }

        }

        ///////////////////////////////////////////
        // areEqual(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public MaybeNotInstantiatedBool areEqual(ITerm left, ITerm right) {
            final Optional<Result<IUnifier.Immutable>> result;
            try {
                result = unify(left, right);
            } catch(OccursException e) {
                return MaybeNotInstantiatedBool.ofResult(false);
            }
            if(!result.isPresent()) {
                // unification failed, not equal
                return MaybeNotInstantiatedBool.ofResult(false);
            }
            final IUnifier.Immutable diff = result.get().result();
            if(diff.isEmpty()) {
                // nothing was unifier, equal
                return MaybeNotInstantiatedBool.ofResult(true);
            }
            return MaybeNotInstantiatedBool.ofNotInstantiated(diff.varSet());
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
            return new PersistentUnifier.Immutable(finite, Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of(),
                    Set.Immutable.of());
        }

        ///////////////////////////////////////////
        // class Transient
        ///////////////////////////////////////////

        private class Transient extends PersistentUnifier implements IUnifier {

            private static final long serialVersionUID = 42L;

            protected final boolean finite = PersistentUnifier.Immutable.this.finite;

            protected final Map.Transient<ITermVar, ITermVar> reps =
                    PersistentUnifier.Immutable.this.reps.get().asTransient();
            protected final Map.Transient<ITermVar, Integer> ranks =
                    PersistentUnifier.Immutable.this.ranks.asTransient();
            protected final Map.Transient<ITermVar, ITerm> terms = PersistentUnifier.Immutable.this.terms.asTransient();

            @Override public boolean isFinite() {
                return finite;
            }

            @Override protected Map<ITermVar, ITermVar> reps() {
                return reps;
            }

            @Override protected Map<ITermVar, ITerm> terms() {
                return terms;
            }

            @Override protected Collection<? extends java.util.Map<ITermVar, ITerm>> disequalities() {
                return disequalities;
            }

            @Override public ITermVar findRep(ITermVar var) {
                return findRep(var, reps);
            }

        }

    }

}