package mb.nabl2.terms.unification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
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

        protected final boolean finite;

        protected final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
        protected final Map.Immutable<ITermVar, Integer> ranks;
        protected final Map.Immutable<ITermVar, ITerm> terms;

        protected Immutable(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms) {
            this.finite = finite;
            this.reps = new Ref<>(reps);
            this.ranks = ranks;
            this.terms = terms;
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

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(ITerm left, ITerm right)
                throws OccursException {
            final IUnifier.Transient unifier = melt();
            return unifier.unify(left, right).map(diff -> {
                return new BaseUnifier.Result<>(diff, unifier.freeze());
            });
        }

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(ITerm left, ITerm right,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidVarsException {
            final IUnifier.Transient unifier = melt();
            return unifier.unify(left, right, isRigid).map(diff -> {
                return new BaseUnifier.Result<>(diff, unifier.freeze());
            });
        }

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(IUnifier other)
                throws OccursException {
            final IUnifier.Transient unifier = melt();
            return unifier.unify(other).map(diff -> {
                return new BaseUnifier.Result<>(diff, unifier.freeze());
            });
        }

        @Override public Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unify(IUnifier other,
                Predicate1<ITermVar> isRigid) throws OccursException, RigidVarsException {
            final IUnifier.Transient unifier = melt();
            return unifier.unify(other, isRigid).map(diff -> {
                return new BaseUnifier.Result<>(diff, unifier.freeze());
            });
        }

        @Override public ITermVar findRep(ITermVar var) {
            final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
            final ITermVar rep = findRep(var, reps);
            this.reps.set(reps.freeze());
            return rep;
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retain(ITermVar var) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.retain(var);
            return new BaseUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> retainAll(Iterable<ITermVar> vars) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.retainAll(vars);
            return new BaseUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> remove(ITermVar var) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.remove(var);
            return new BaseUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Immutable.Result<ISubstitution.Immutable> removeAll(Iterable<ITermVar> vars) {
            final IUnifier.Transient unifier = melt();
            ISubstitution.Immutable result = unifier.removeAll(vars);
            return new BaseUnifier.Result<>(result, unifier.freeze());
        }

        @Override public IUnifier.Transient melt() {
            return new PersistentUnifier.Transient(finite, reps.get().asTransient(), ranks.asTransient(),
                    terms.asTransient());
        }

        public static IUnifier.Immutable of() {
            return of(true);
        }

        public static IUnifier.Immutable of(boolean finite) {
            return new PersistentUnifier.Immutable(finite, Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }

    }

    ///////////////////////////////////////////
    // class Transient
    ///////////////////////////////////////////

    public static class Transient extends PersistentUnifier implements IUnifier.Transient, Serializable {

        private static final long serialVersionUID = 42L;

        protected final boolean finite;

        protected final Map.Transient<ITermVar, ITermVar> reps;
        protected final Map.Transient<ITermVar, Integer> ranks;
        protected final Map.Transient<ITermVar, ITerm> terms;

        protected Transient(final boolean finite, final Map.Transient<ITermVar, ITermVar> reps,
                final Map.Transient<ITermVar, Integer> ranks, final Map.Transient<ITermVar, ITerm> terms) {
            this.finite = finite;
            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;
        }

        @Override public boolean isFinite() {
            return finite;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps;
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override public ITermVar findRep(ITermVar var) {
            return findRep(var, reps);
        }

        @Override public PersistentUnifier.Immutable freeze() {
            return new PersistentUnifier.Immutable(finite, reps.freeze(), ranks.freeze(), terms.freeze());
        }

        public static IUnifier.Transient of() {
            return of(true);
        }

        public static IUnifier.Transient of(boolean finite) {
            return new PersistentUnifier.Transient(finite, Map.Transient.of(), Map.Transient.of(), Map.Transient.of());
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Optional<IUnifier.Immutable> unify(ITerm left, ITerm right) throws OccursException {
            try {
                return new Unify(left, right).apply();
            } catch(RigidVarsException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override public Optional<IUnifier.Immutable> unify(ITerm left, ITerm right, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidVarsException {
            return new Unify(left, right, isRigid).apply();
        }

        @Override public Optional<IUnifier.Immutable> unify(IUnifier other) throws OccursException {
            try {
                return new Unify(other).apply();
            } catch(RigidVarsException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override public Optional<IUnifier.Immutable> unify(IUnifier other, Predicate1<ITermVar> isRigid)
                throws OccursException {
            try {
                return new Unify(other, isRigid).apply();
            } catch(RigidVarsException e) {
                throw new IllegalStateException(e);
            }
        }

        private class Unify {

            private final Predicate1<ITermVar> isRigid;

            public final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            public final Set<ITermVar> result = Sets.newHashSet();

            public Unify(ITerm left, ITerm right) {
                this(left, right, v -> false);
            }

            public Unify(ITerm left, ITerm right, Predicate1<ITermVar> isRigid) {
                this.isRigid = isRigid;
                worklist.push(ImmutableTuple2.of(left, right));
            }

            public Unify(IUnifier other) {
                this(other, v -> false);
            }

            public Unify(IUnifier other, Predicate1<ITermVar> isRigid) {
                this.isRigid = isRigid;
                for(ITermVar var : other.varSet()) {
                    final ITermVar rep = other.findRep(var);
                    if(!var.equals(rep)) {
                        worklist.push(ImmutableTuple2.of(var, rep));
                    } else {
                        final ITerm term = other.findTerm(var);
                        if(!var.equals(term)) {
                            worklist.push(ImmutableTuple2.of(var, term));
                        }
                    }
                }
            }

            public Optional<IUnifier.Immutable> apply() throws OccursException, RigidVarsException {
                while(!worklist.isEmpty()) {
                    final Tuple2<ITerm, ITerm> work = worklist.pop();
                    try {
                        if(!unifyTerms(work._1(), work._2())) {
                            return Optional.empty();
                        }
                    } catch(_RigidVarsException ex) {
                        throw ex.exception;
                    }
                }
                if(isFinite()) {
                    final Set<ITermVar> cyclicVars =
                            result.stream().filter(v -> isCyclic(v)).collect(ImmutableSet.toImmutableSet());
                    if(!cyclicVars.isEmpty()) {
                        throw new OccursException(cyclicVars);
                    }
                }
                return Optional.of(diffUnifier(result));
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
                java.util.Map<ITermVar, ITerm> target = allowCrossModuleUnification() ? targetTerms(rep) : terms;
                if (target.containsKey(rep)) {
                    worklist.push(ImmutableTuple2.of(target.get(rep), term));
                } else if(isRigid.test(rep)) {
                    throw new _RigidVarsException(rep);
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
                if(isRigid.test(leftRep) && isRigid.test(rightRep)) {
                    throw new _RigidVarsException(leftRep, rightRep);
                }
                final int leftRank = ranks.getOrDefault(leftRep, 1);
                final int rightRank = ranks.getOrDefault(rightRep, 1);
                final boolean swap;
                if(isRigid.test(leftRep)) {
                    swap = true;
                } else if(isRigid.test(rightRep)) {
                    swap = false;
                } else {
                    swap = leftRank > rightRank;
                }
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

        }

        ///////////////////////////////////////////
        // retain(ITermVar)
        ///////////////////////////////////////////

        @Override public ISubstitution.Immutable retain(ITermVar var) {
            return retainAll(Collections.singleton(var));
        }

        @Override public ISubstitution.Immutable retainAll(Iterable<ITermVar> vars) {
            return removeAll(Sets.difference(ImmutableSet.copyOf(varSet()), ImmutableSet.copyOf(vars)));
        }

        ///////////////////////////////////////////
        // remove(ITermVar)
        ///////////////////////////////////////////

        @Override public ISubstitution.Immutable removeAll(Iterable<ITermVar> vars) {
            final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
            for(ITermVar var : vars) {
                subst.compose(remove(var));
            }
            return subst.freeze();
        }

        @Override public ISubstitution.Immutable remove(ITermVar var) {
            final ISubstitution.Immutable subst;
            if(reps.containsKey(var)) { // var |-> rep
                final ITermVar rep = reps.__remove(var);
                CapsuleUtil.replace(reps, (v, r) -> r.equals(var) ? rep : r);
                subst = PersistentSubstitution.Immutable.of(var, rep);
            } else {
                final Optional<ITermVar> maybeNewRep =
                        reps.entrySet().stream().filter(e -> e.getValue().equals(var)).map(e -> e.getKey()).findAny();
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

        ///////////////////////////////////////////
        // diffUnifier(Set<ITermVar>)
        ///////////////////////////////////////////

        private IUnifier.Immutable diffUnifier(Set<ITermVar> vars) {
            final Map.Transient<ITermVar, ITermVar> diffReps = Map.Transient.of();
            final Map.Transient<ITermVar, ITerm> diffTerms = Map.Transient.of();
            for(ITermVar var : vars) {
                if(reps.containsKey(var)) {
                    diffReps.__put(var, reps.get(var));
                } else if(terms.containsKey(var)) {
                    diffTerms.__put(var, terms.get(var));
                }
            }
            return new PersistentUnifier.Immutable(finite, diffReps.freeze(), Map.Immutable.of(), diffTerms.freeze());
        }

    }

}