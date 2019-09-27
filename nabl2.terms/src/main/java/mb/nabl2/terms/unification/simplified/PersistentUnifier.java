package mb.nabl2.terms.unification.simplified;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.metaborg.util.Ref;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.ImmutableTuple2;

public class PersistentUnifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    private final boolean finite;

    private final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
    private final Map.Immutable<ITermVar, Integer> ranks;
    private final Map.Immutable<ITermVar, ITerm> terms;

    public PersistentUnifier(final boolean finite, final Map.Immutable<ITermVar, ITermVar> reps,
            final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms) {
        this.finite = finite;
        this.reps = new Ref<>(reps);
        this.ranks = ranks;
        this.terms = terms;
    }

    @Override public Set<ITermVar> varSet() {
        return Sets.union(reps.get().keySet(), terms.keySet()).immutableCopy();
    }

    @Override public Set<ITermVar> freeVarSet() {
        final Set<ITermVar> vars = varSet();
        // @formatter:off
        return Stream.concat(reps.get().values().stream(), terms.values().stream())
            .flatMap(t -> t.getVars().stream())
            .filter(v -> !vars.contains(v))
            .collect(ImmutableSet.toImmutableSet());
        // @formatter:on
    }

    @Override public Iterable<? extends Entry<ITermVar, ? extends ITerm>> entries() {
        return Iterables.concat(reps.get().entrySet(), terms.entrySet());
    }

    ///////////////////////////////////////////
    // isCyclic
    ///////////////////////////////////////////

    @Override public boolean isCyclic(ITerm term) {
        return new IsCyclic().apply(term);
    }

    private class IsCyclic extends VarFold<Boolean> {

        @Override protected Boolean combine(List<Boolean> ts) {
            return ts.stream().anyMatch(r -> r);
        }

        @Override protected Boolean leaf() {
            return false;
        }

        @Override protected Boolean var(String name, boolean rec) {
            return rec;
        }

    }

    ///////////////////////////////////////////
    // isGround
    ///////////////////////////////////////////

    @Override public boolean isGround(ITerm term) {
        return new IsGround().apply(term);
    }

    private class IsGround extends VarFold<Boolean> {

        @Override protected Boolean combine(List<Boolean> ts) {
            return ts.stream().allMatch(r -> r);
        }

        @Override protected Boolean leaf() {
            return true;
        }

        @Override protected Boolean var(String name, boolean rec) {
            return rec;
        }

    }

    ///////////////////////////////////////////
    // find
    ///////////////////////////////////////////

    @Override public ITerm find(ITerm term) {
        return term.match(Terms.<ITerm>cases().var(var -> {
            final ITermVar rep = findRep(var);
            return terms.getOrDefault(rep, rep);
        }).otherwise(t -> t));
    }

    private ITermVar findRep(ITermVar var) {
        final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
        final ITermVar rep = findAndUpdateReps(var, reps);
        this.reps.set(reps.freeze());
        return rep;
    }

    ///////////////////////////////////////////
    // unifyAll
    ///////////////////////////////////////////

    @Override public Optional<Result<PersistentUnifier>>
            unifyAll(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) throws OccursException {
        return new UnifyAll(equalities).apply();
    }

    private class UnifyAll extends Transient {

        private final Deque<Entry<? extends ITerm, ? extends ITerm>> worklist = Lists.newLinkedList();
        private final List<ITermVar> result = Lists.newArrayList();

        public UnifyAll(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) {
            equalities.forEach(e -> worklist.push(e));
        }

        public Optional<Result<PersistentUnifier>> apply() throws OccursException {
            while(!worklist.isEmpty()) {
                final Entry<? extends ITerm, ? extends ITerm> work = worklist.pop();
                if(!unifyTerms(work.getKey(), work.getValue())) {
                    return Optional.empty();
                }
            }

            if(finite) {
                final ImmutableSet<ITermVar> cyclicVars =
                        result.stream().filter(v -> isCyclic(v)).collect(ImmutableSet.toImmutableSet());
                if(!cyclicVars.isEmpty()) {
                    throw new OccursException(cyclicVars);
                }
            }

            final PersistentUnifier unifier =
                    new PersistentUnifier(finite, reps.freeze(), ranks.freeze(), terms.freeze());
            final PersistentUnifier diff = diff(result);
            return Optional.of(new Result<>(diff, unifier));
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
            final ITerm repTerm = terms.get(rep); // term for the representative
            if(repTerm != null) {
                worklist.push(ImmutableTuple2.of(repTerm, term));
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
            final int leftRank = Optional.ofNullable(ranks.__remove(leftRep)).orElse(1);
            final int rightRank = Optional.ofNullable(ranks.__remove(rightRep)).orElse(1);
            final boolean swap = leftRank > rightRank;
            final ITermVar var = swap ? rightRep : leftRep; // the eliminated variable
            final ITermVar rep = swap ? leftRep : rightRep; // the new representative
            ranks.__put(rep, leftRank + rightRank);
            reps.__put(var, rep);
            final ITerm varTerm = terms.__remove(var); // term for the eliminated var
            if(varTerm != null) {
                final ITerm repTerm = terms.get(rep); // term for the represenative
                if(repTerm != null) {
                    worklist.push(ImmutableTuple2.of(varTerm, repTerm));
                    // don't add to result
                } else {
                    terms.__put(rep, varTerm);
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

        private PersistentUnifier diff(Iterable<ITermVar> vars) {
            final Map.Transient<ITermVar, ITermVar> diffReps = Map.Transient.of();
            final Map.Transient<ITermVar, ITerm> diffTerms = Map.Transient.of();
            for(ITermVar var : vars) {
                if(reps.containsKey(var)) {
                    diffReps.__put(var, reps.get(var));
                } else if(terms.containsKey(var)) {
                    diffTerms.__put(var, terms.get(var));
                }
            }
            return new PersistentUnifier(finite, diffReps.freeze(), Map.Immutable.of(), diffTerms.freeze());
        }

    }

    ///////////////////////////////////////////
    // removeAll
    ///////////////////////////////////////////

    @Override public Result<Immutable> removeAll(Iterable<ITermVar> vars) {
        return new RemoveAll(vars).apply();

    }

    private class RemoveAll extends Transient {

        private final java.util.Set<ITermVar> vars;

        public RemoveAll(Iterable<ITermVar> vars) {
            this.vars = ImmutableSet.copyOf(vars);
        }

        public Result<ISubstitution.Immutable> apply() {
            final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
            // remove vars from unifier
            for(ITermVar var : vars) {
                subst.compose(remove(var));
            }

            final java.util.Set<ITermVar> escapedVars = Sets.intersection(freeVarSet(), vars).immutableCopy();
            if(!escapedVars.isEmpty()) {
                throw new IllegalArgumentException("Variables escapes removal: " + escapedVars);
            }

            final PersistentUnifier newUnifier =
                    new PersistentUnifier(finite, reps.freeze(), ranks.freeze(), terms.freeze());
            return new Result<>(subst.freeze(), newUnifier);
        }

        private ISubstitution.Immutable remove(ITermVar var) {
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

    }

    ///////////////////////////////////////////
    // auxiliary
    ///////////////////////////////////////////

    public static class Result<R> implements IUnifier.Result<R> {

        private final R result;
        private final PersistentUnifier unifier;

        private Result(R result, PersistentUnifier unifier) {
            this.result = result;
            this.unifier = unifier;
        }

        @Override public R result() {
            return result;
        }

        @Override public PersistentUnifier unifier() {
            return unifier;
        }

    }

    /**
     * Base class for mutating operations.
     */
    private class Transient {

        protected final Map.Transient<ITermVar, ITermVar> reps = PersistentUnifier.this.reps.get().asTransient();
        protected final Map.Transient<ITermVar, Integer> ranks = PersistentUnifier.this.ranks.asTransient();
        protected final Map.Transient<ITermVar, ITerm> terms = PersistentUnifier.this.terms.asTransient();

        protected ITermVar findRep(ITermVar var) {
            return findAndUpdateReps(var, reps);
        }

    }

    private abstract class VarFold<T> extends TermFold<T> {

        protected abstract T leaf();

        protected abstract T combine(List<T> ts);

        @Override protected T appl(String op, List<T> ts) {
            return combine(ts);
        }

        @Override protected T list(List<T> ts) {
            return combine(ts);
        }

        @Override protected T listTail(List<T> ts, T t) {
            return combine(Arrays.asList(combine(ts), t));
        }

        @Override protected T string(String value) {
            return leaf();
        }

        @Override protected T integer(int value) {
            return leaf();
        }

        @Override protected T blob(Object value) {
            return leaf();
        }

    }

    private abstract class TermFold<T> {

        protected abstract T appl(String op, List<T> ts);

        protected abstract T list(List<T> ts);

        protected abstract T listTail(List<T> ts, T t);

        protected abstract T string(String value);

        protected abstract T integer(int value);

        protected abstract T blob(Object value);

        protected abstract T var(String name, boolean rec);

        protected T fix(T f, T t) {
            return t;
        }

        protected Optional<T> cutoff(int i) {
            return Optional.empty();
        }

        public T apply(ITerm term) {
            return fold(term, Maps.newHashMap(), Maps.newHashMap(), 0);
        }

        private T fold(final ITerm term, final java.util.Map<ITermVar, T> stack,
                final java.util.Map<ITermVar, T> visited, final int depth) {
            final Optional<T> cutoff = cutoff(depth);
            if(cutoff.isPresent()) {
                return cutoff.get();
            }
            // @formatter:off
            return term.match(Terms.<T>cases(
                appl -> appl(appl.getOp(), folds(appl.getArgs(), stack, visited, depth - 1)),
                list -> fold(list, stack, visited, depth),
                string -> string(string.getValue()),
                integer -> integer(integer.getValue()),
                blob -> blob(blob.getValue()),
                var -> fold(var, stack, visited, depth)
            ));
            // @formatter:on
        }

        private T fold(IListTerm list, final java.util.Map<ITermVar, T> stack, final java.util.Map<ITermVar, T> visited,
                final int depth) {
            final Optional<T> cutoff = cutoff(depth);
            if(cutoff.isPresent()) {
                return cutoff.get();
            }
            final List<T> ts = Lists.newArrayList();
            final Ref<T> t = new Ref<>();
            while(list != null) {
                // @formatter:off
                list = list.match(ListTerms.cases(
                    cons -> {
                        ts.add(fold(cons.getHead(), stack, visited, depth - 1));
                        return cons.getTail();
                    },
                    nil -> {
                        return null;
                    },
                    var -> {
                        t.set(fold(var, stack, visited, depth - 1));
                        return null;
                    }
                ));
                // @formatter:on
            }
            return t.get() != null ? listTail(ts, t.get()) : list(ts);
        }

        private T fold(final ITermVar var, final java.util.Map<ITermVar, T> stack,
                final java.util.Map<ITermVar, T> visited, final int depth) {
            final Optional<T> cutoff = cutoff(depth);
            if(cutoff.isPresent()) {
                return cutoff.get();
            }
            final ITermVar rep = findRep(var);
            final T t;
            if(!visited.containsKey(rep)) {
                stack.put(rep, null);
                visited.put(rep, null);
                final ITerm term = terms.get(rep);
                if(term != null) {
                    final T tt = fold(term, stack, visited, depth);
                    t = (stack.get(rep) != null) ? fix(stack.get(rep), tt) : tt;
                } else {
                    t = var(rep.getName(), false);
                }
                visited.put(rep, t);
                stack.remove(rep);
                return t;
            } else if(stack.containsKey(rep)) {
                if(stack.get(rep) == null) {
                    t = var(rep.getName(), true);
                    stack.put(rep, t);
                } else {
                    t = stack.get(rep);
                }
            } else {
                t = visited.get(rep);
            }
            return t;
        }

        private List<T> folds(final Iterable<ITerm> terms, final java.util.Map<ITermVar, T> stack,
                final java.util.Map<ITermVar, T> visited, final int depth) {
            final List<T> ts = Lists.newArrayList();
            for(ITerm term : terms) {
                ts.add(fold(term, stack, visited, depth));
            }
            return ts;
        }

    }

    private static ITermVar findAndUpdateReps(ITermVar var, Map.Transient<ITermVar, ITermVar> reps) {
        ITermVar rep = reps.get(var);
        if(rep == null) {
            return var;
        } else {
            rep = findAndUpdateReps(rep, reps);
            reps.__put(var, rep);
            return rep;
        }
    }

}
