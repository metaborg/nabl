package mb.nabl2.terms.unification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class MutableUnifier extends BaseUnifier implements IUnifier.Transient, Serializable {

    private static final long serialVersionUID = 42L;

    private final boolean finite;

    private final Map<ITermVar, ITermVar> reps;
    private final Map<ITermVar, Integer> ranks;
    private final Map<ITermVar, ITerm> terms;

    public MutableUnifier(final boolean finite, final Map<ITermVar, ITermVar> reps, final Map<ITermVar, Integer> ranks,
            final Map<ITermVar, ITerm> terms) {
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

    protected static ITermVar findRep(ITermVar var, Map<ITermVar, ITermVar> reps) {
        ITermVar rep = reps.get(var);
        if(rep == null) {
            return var;
        } else {
            rep = findRep(rep, reps);
            reps.put(var, rep);
            return rep;
        }
    }

    @Override public IUnifier.Immutable freeze() {
        return new PersistentUnifier.Immutable(finite,
                io.usethesource.capsule.Map.Immutable.<ITermVar, ITermVar>of().__putAll(reps),
                io.usethesource.capsule.Map.Immutable.<ITermVar, Integer>of().__putAll(ranks),
                io.usethesource.capsule.Map.Immutable.<ITermVar, ITerm>of().__putAll(terms));
    }

    public static IUnifier.Transient of() {
        return of(true);
    }

    public static IUnifier.Transient of(boolean finite) {
        return new MutableUnifier(finite, new HashMap<>(), new HashMap<>(), new HashMap<>());
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
            if(terms.containsKey(rep)) {
                worklist.push(ImmutableTuple2.of(terms.get(rep), term));
            } else if(isRigid.test(rep)) {
                throw new _RigidVarsException(rep);
            } else {
                terms.put(rep, term);
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
            ranks.put(with, leftRank + rightRank);
            reps.put(var, with);
            result.add(var);
            final ITerm term = terms.remove(var); // term for the eliminated var
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
            final ITermVar rep = reps.remove(var);
            reps.replaceAll((v, r) -> r.equals(var) ? rep : r);
            subst = PersistentSubstitution.Immutable.of(var, rep);
        } else {
            final Optional<ITermVar> maybeNewRep =
                    reps.entrySet().stream().filter(e -> e.getValue().equals(var)).map(e -> e.getKey()).findAny();
            if(maybeNewRep.isPresent()) { // newRep |-> var
                final ITermVar newRep = maybeNewRep.get();
                reps.remove(newRep);
                reps.replaceAll((v, r) -> r.equals(var) ? newRep : r);
                if(terms.containsKey(var)) { // var -> term
                    final ITerm term = terms.remove(var);
                    terms.put(newRep, term);
                }
                subst = PersistentSubstitution.Immutable.of(var, newRep);
            } else {
                if(terms.containsKey(var)) { // var -> term
                    final ITerm term = terms.remove(var);
                    subst = PersistentSubstitution.Immutable.of(var, term);
                } else { // var free -- cannot eliminate
                    subst = PersistentSubstitution.Immutable.of();
                }
            }
        }
        terms.replaceAll((v, t) -> subst.apply(t));
        return subst;
    }

    ///////////////////////////////////////////
    // diffUnifier(Set<ITermVar>)
    ///////////////////////////////////////////

    private IUnifier.Immutable diffUnifier(Set<ITermVar> vars) {
        final io.usethesource.capsule.Map.Transient<ITermVar, ITermVar> diffReps =
                io.usethesource.capsule.Map.Transient.of();
        final io.usethesource.capsule.Map.Transient<ITermVar, ITerm> diffTerms =
                io.usethesource.capsule.Map.Transient.of();
        for(ITermVar var : vars) {
            if(reps.containsKey(var)) {
                diffReps.__put(var, reps.get(var));
            } else if(terms.containsKey(var)) {
                diffTerms.__put(var, terms.get(var));
            }
        }
        return new PersistentUnifier.Immutable(finite, diffReps.freeze(), io.usethesource.capsule.Map.Immutable.of(),
                diffTerms.freeze());
    }


}