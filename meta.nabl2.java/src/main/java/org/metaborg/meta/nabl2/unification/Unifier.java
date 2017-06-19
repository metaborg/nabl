package org.metaborg.meta.nabl2.unification;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import io.usethesource.capsule.Map;

public abstract class Unifier implements IUnifier {

    protected Unifier() {
    }

    protected abstract Map<ITermVar, ITerm> reps();

    /**
     * Find representative term.
     */
    @Override public ITerm find(ITerm term) {
        // @formatter:off
        return term.isGround() ? term : term.match(Terms.<ITerm>cases(
            (appl) -> TB.newAppl(appl.getOp(), appl.getArgs().stream().map(this::find).collect(Collectors.toList()), appl.getAttachments()),
            (list) -> find(list),
            (string) -> string,
            (integer) -> integer,
            (var) -> findVarRep(var)
        ));
        // @formatter:on
    }

    public IListTerm find(IListTerm list) {
        // @formatter:off
        IListTerm rep = list.isGround() ? list : list.match(ListTerms.<IListTerm>cases(
            (cons) -> TB.newCons(find(cons.getHead()), find(cons.getTail()), cons.getAttachments()),
            (nil) -> nil,
            (var) -> (IListTerm) findVarRep(var)
        ));
        // @formatter:on
        if(list.isLocked()) {
            rep = rep.withLocked(true);
        }
        return rep;
    }

    protected abstract ITerm findVarRep(ITermVar var);

    /**
     * Find representative term, without recursing on subterms.
     */
    protected ITerm findShallow(ITerm term) {
        ITerm rep = M.var(this::findVarRepShallow).match(term).orElse(term);
        if(term.isLocked()) {
            rep = rep.withLocked(true);
        }
        return rep;
    }

    protected abstract ITerm findVarRepShallow(ITermVar var);

    @Override public Stream<Tuple2<ITermVar, ITerm>> stream() {
        return reps().entrySet().stream().map(kv -> ImmutableTuple2.of(kv.getKey(), find(kv.getValue())));
    }

    public static class Immutable extends Unifier implements IUnifier.Immutable, Serializable {
        private static final long serialVersionUID = 42L;

        private final Map.Immutable<ITermVar, ITerm> reps;
        private final Map.Immutable<ITermVar, Integer> sizes;

        private Immutable(Map.Immutable<ITermVar, ITerm> reps, Map.Immutable<ITermVar, Integer> sizes) {
            this.reps = reps;
            this.sizes = sizes;
        }

        @Override protected Map<ITermVar, ITerm> reps() {
            return reps;
        }

        @Override public Set<ITermVar> getAllVars() {
            return reps.keySet();
        }

        @Override protected ITerm findVarRep(ITermVar var) {
            if(!reps.containsKey(var)) {
                return var;
            } else {
                ITerm rep = find(reps.get(var));
                return rep;
            }
        }

        @Override protected ITerm findVarRepShallow(ITermVar var) {
            if(!reps.containsKey(var)) {
                return var;
            } else {
                ITerm rep = findShallow(reps.get(var));
                return rep;
            }
        }

        @Override public Unifier.Transient melt() {
            return new Unifier.Transient(reps.asTransient(), sizes.asTransient());
        }

        public static Unifier.Immutable of() {
            return new Unifier.Immutable(Map.Immutable.of(), Map.Immutable.of());
        }

    }

    public static class Transient extends Unifier implements IUnifier.Transient {

        private final Map.Transient<ITermVar, ITerm> reps;
        private final Map.Transient<ITermVar, Integer> sizes;

        private Transient(Map.Transient<ITermVar, ITerm> reps, Map.Transient<ITermVar, Integer> sizes) {
            this.reps = reps;
            this.sizes = sizes;
        }

        @Override public Set<ITermVar> getAllVars() {
            return reps.keySet();
        }

        @Override protected ITerm findVarRep(ITermVar var) {
            if(!reps.containsKey(var)) {
                return var;
            } else {
                ITerm rep = find(reps.get(var));
                reps.__put(var, rep);
                return rep;
            }
        }

        @Override protected ITerm findVarRepShallow(ITermVar var) {
            if(!reps.containsKey(var)) {
                return var;
            } else {
                ITerm rep = findShallow(reps.get(var));
                reps.__put(var, rep);
                return rep;
            }
        }

        /**
         * Unify two terms.
         * 
         * @return Unified term
         */
        @Override public UnificationResult unify(ITerm left, ITerm right) throws UnificationException {
            final UnificationResult result = new UnificationResult();
            if(!unifyTerms(left, right, result)) {
                throw new UnificationException(find(left), find(right));
            }
            return result;
        }

        @Override protected Map<ITermVar, ITerm> reps() {
            return reps;
        }

        private boolean unifyTerms(ITerm left, ITerm right, UnificationResult result) {
            ITerm leftRep = findShallow(left);
            ITerm rightRep = findShallow(right);
            if(leftRep.equals(rightRep)) {
                return true;
            } else if(leftRep.isGround() && rightRep.isGround()) {
                return false;
            }
            // @formatter:off
            return leftRep.match(Terms.cases(
                applLeft -> M.cases(
                    M.appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                        applLeft.getArity() == applRight.getArity() &&
                                        unifys(applLeft.getArgs(), applRight.getArgs(), result)),
                    M.var(varRight -> unifyVarTerm(varRight, applLeft, result))
                ).match(rightRep).orElse(false),
                listLeft -> M.cases(
                    M.list(listRight -> listLeft.match(ListTerms.cases(
                        consLeft -> M.cases(
                            M.cons(consRight -> {
                                return unifyTerms(consLeft.getHead(), consRight.getHead(), result) &&
                                       unifyTerms(consLeft.getTail(), consRight.getTail(), result);
                            }),
                            M.var(varRight -> unifyVarTerm(varRight, consLeft, result))
                        ).match(listRight).orElse(false),
                        nilLeft -> M.cases(
                            M.nil(nilRight -> true),
                            M.var(varRight -> unifyVarTerm(varRight, nilLeft, result))
                        ).match(listRight).orElse(false),
                        varLeft -> M.cases(
                            M.var(varRight -> unifyVars(varLeft, varRight, result)),
                            M.term(termRight -> unifyVarTerm(varLeft, termRight, result))
                        ).match(listRight).orElse(false)
                    ))),
                    M.var(varRight -> unifyVarTerm(varRight, listLeft, result))
                ).match(rightRep).orElse(false),
                stringLeft -> M.cases(
                    M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                    M.var(varRight -> unifyVarTerm(varRight, stringLeft, result))
                ).match(rightRep).orElse(false),
                integerLeft -> M.cases(
                    M.integer(integerRight -> integerLeft.getValue() != integerRight.getValue()),
                    M.var(varRight -> unifyVarTerm(varRight, integerLeft, result))
                ).match(rightRep).orElse(false),
                varLeft -> M.cases(
                    // match var before term, or term will always match
                    M.var(varRight -> unifyVars(varLeft, varRight, result)),
                    M.term(termRight -> unifyVarTerm(varLeft, termRight, result))
                ).match(rightRep).orElse(false)
            ));
            // @formatter:on
        }

        private boolean unifyVarTerm(ITermVar var, ITerm term, UnificationResult result) {
            if(term.getVars().contains(var)) {
                return false;
            }
            if(var.isLocked()) {
                result.addResidual(var, term);
            } else {
                reps.put(var, term);
                result.addSubstituted(var);
            }
            return true;
        }

        private boolean unifyVars(ITermVar varLeft, ITermVar varRight, UnificationResult result) {
            if(varLeft.isLocked() && varRight.isLocked()) {
                result.addResidual(varLeft, varRight);
            } else {
                final boolean swap;
                if(varLeft.isLocked()) {
                    swap = true;
                } else if(varRight.isLocked()) {
                    swap = false;
                } else {
                    final int sizeLeft = sizes.getOrDefault(varLeft, 1);
                    final int sizeRight = sizes.getOrDefault(varRight, 1);
                    swap = sizeLeft > sizeRight;
                }
                final ITermVar var = swap ? varRight : varLeft;
                final ITermVar with = swap ? varLeft : varRight;
                {
                    int sizeLeft = sizes.getOrDefault(var, 1);
                    int sizeRight = sizes.getOrDefault(with, 1);
                    sizes.put(with, sizeLeft + sizeRight);
                    reps.put(var, with);
                    result.addSubstituted(var);
                }

            }
            return true;
        }

        private boolean unifys(Iterable<ITerm> lefts, Iterable<ITerm> rights, UnificationResult result) {
            Iterator<ITerm> itLeft = lefts.iterator();
            Iterator<ITerm> itRight = rights.iterator();
            boolean success = true;
            while(itLeft.hasNext()) {
                if(!itRight.hasNext()) {
                    return false;
                }
                success &= unifyTerms(itLeft.next(), itRight.next(), result);
            }
            if(itRight.hasNext()) {
                return false;
            }
            return success;
        }

        @Override public IUnifier.Immutable freeze() {
            reps.keySet().stream().forEach(this::find);
            return new Unifier.Immutable(reps.freeze(), sizes.freeze());
        }

        public static Unifier.Transient of() {
            return new Unifier.Transient(Map.Transient.of(), Map.Transient.of());
        }

    }

    public static IUnifier.Immutable findAndLock(IUnifier unifier) {
        final Map.Transient<ITermVar, ITerm> reps = Map.Transient.of();
        unifier.stream().forEach(vt -> reps.__put(vt._1(), unifier.find(vt._2()).withLocked(true)));
        return new Unifier.Immutable(reps.freeze(), Map.Immutable.of());
    }

}