package org.metaborg.meta.nabl2.unification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.collections.HashRelation3;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class Unifier<D, E> implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<ITermVar, ITerm> reps;
    private final Map<ITermVar, Integer> sizes;

    private final SetMultimap<ITermVar, D> activeVars;
    private final Set<ITermVar> frozenVars;

    private final IRelation3.Mutable<ITermVar, ITerm, E> varDetermined;

    public Unifier() {
        this.reps = Maps.newHashMap();
        this.sizes = Maps.newHashMap();
        this.activeVars = HashMultimap.create();
        this.frozenVars = Sets.newHashSet();
        this.varDetermined = HashRelation3.create();
    }

    @Override public Set<ITermVar> getAllVars() {
        return Collections.unmodifiableSet(reps.keySet());
    }

    /**
     * Find representative term.
     */
    public ITerm find(ITerm term) {
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
        return list.isGround() ? list : list.match(ListTerms.<IListTerm>cases(
            (cons) -> TB.newCons(find(cons.getHead()), find(cons.getTail()), cons.getAttachments()),
            (nil) -> nil,
            (var) -> (IListTerm) findVarRep(var)
        ));
        // @formatter:on
    }

    private ITerm findVarRep(ITermVar var) {
        if(!reps.containsKey(var)) {
            return var;
        } else {
            ITerm rep = find(reps.get(var));
            reps.put(var, rep);
            return rep;
        }
    }

    /**
     * Find representative term, without recursing on subterms.
     */
    private ITerm findShallow(ITerm term) {
        return M.var(this::findVarRepShallow).match(term).orElse(term);
    }

    private ITerm findVarRepShallow(ITermVar var) {
        if(!reps.containsKey(var)) {
            return var;
        } else {
            ITerm rep = findShallow(reps.get(var));
            reps.put(var, rep);
            return rep;
        }
    }

    /**
     * Unify two terms.
     * 
     * @return Unified term
     */
    public void unify(ITerm left, ITerm right) throws UnificationException {
        if(!unifyTerms(left, right)) {
            throw new UnificationException(find(left), find(right));
        }
    }

    private boolean unifyTerms(ITerm left, ITerm right) {
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
                                    unifys(applLeft.getArgs(), applRight.getArgs())),
                M.var(varRight -> unifyVarTerm(varRight, applLeft))
            ).match(rightRep).orElse(false),
            listLeft -> M.cases(
                M.list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> M.cases(
                        M.cons(consRight -> {
                            return unifyTerms(consLeft.getHead(), consRight.getHead()) &&
                                   unifyTerms(consLeft.getTail(), consRight.getTail());
                        }),
                        M.var(varRight -> unifyVarTerm(varRight, consLeft))
                    ).match(listRight).orElse(false),
                    nilLeft -> M.cases(
                        M.nil(nilRight -> true),
                        M.var(varRight -> unifyVarTerm(varRight, nilLeft))
                    ).match(listRight).orElse(false),
                    varLeft -> M.cases(
                        M.var(varRight -> unifyVars(varLeft, varRight)),
                        M.term(termRight -> unifyVarTerm(varLeft, termRight))
                    ).match(listRight).orElse(false)
                ))),
                M.var(varRight -> unifyTerms(varRight, listLeft))
            ).match(rightRep).orElse(false),
            stringLeft -> M.cases(
                M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                M.var(varRight -> unifyVarTerm(varRight, stringLeft))
            ).match(rightRep).orElse(false),
            integerLeft -> M.cases(
                M.integer(integerRight -> integerLeft.getValue() != integerRight.getValue()),
                M.var(varRight -> unifyVarTerm(varRight, integerLeft))
            ).match(rightRep).orElse(false),
            varLeft -> M.cases(
                M.var(varRight -> unifyVars(varLeft, varRight)),
                M.term(termRight -> unifyVarTerm(varLeft, termRight))
            ).match(rightRep).orElse(false)
        ));
        // @formatter:on
    }

    private boolean unifyVarTerm(ITermVar var, ITerm term) {
        if(term.getVars().contains(var)) {
            return false;
        }
        reps.put(var, term);
        updateActive(var, term);
        updateDetermination(var, term);
        return true;
    }

    private boolean unifyVars(ITermVar varLeft, ITermVar varRight) {
        int sizeLeft = sizes.getOrDefault(varLeft, 1);
        int sizeRight = sizes.getOrDefault(varRight, 1);
        if(sizeLeft > sizeRight) {
            reps.put(varRight, varLeft);
            sizes.put(varLeft, sizeLeft + sizeRight);
            updateActive(varRight, varLeft);
            updateDetermination(varRight, varLeft);
        } else {
            reps.put(varLeft, varRight);
            sizes.put(varRight, sizeLeft + sizeRight);
            updateActive(varLeft, varRight);
            updateDetermination(varLeft, varRight);
        }
        return true;
    }

    private boolean unifys(Iterable<ITerm> lefts, Iterable<ITerm> rights) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        boolean success = true;
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return false;
            }
            success &= unifyTerms(itLeft.next(), itRight.next());
        }
        if(itRight.hasNext()) {
            return false;
        }
        return success;
    }


    public boolean canUnify(ITerm left, ITerm right) {
        // @formatter:off
        return left.match(Terms.cases(
            applLeft -> M.cases(
                M.appl(applRight -> (applLeft.getOp().equals(applRight.getOp()) &&
                                     applLeft.getArity() == applLeft.getArity() &&
                                     canUnifys(applLeft.getArgs(), applRight.getArgs()))),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            listLeft -> M.cases(
                M.list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> M.cases(
                        M.cons(consRight -> (canUnify(consLeft.getHead(), consRight.getHead()) &&
                                             canUnify(consLeft.getTail(), consRight.getTail()))),
                        M.var(varRight -> true)
                    ).match(listRight).orElse(false),
                    nilLeft -> M.cases(
                        M.nil(nilRight -> true),
                        M.var(varRight -> true)
                    ).match(listRight).orElse(false),
                    varLeft -> true
                ))),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            stringLeft -> M.cases(
                M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            integerLeft -> M.cases(
                M.integer(integerRight -> (integerLeft.getValue() == integerRight.getValue())),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            varLeft -> true
        ));
        // @formatter:on
    }

    private boolean canUnifys(Iterable<ITerm> lefts, Iterable<ITerm> rights) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!(itRight.hasNext() && canUnify(itLeft.next(), itRight.next()))) {
                return false;
            }
        }
        return !itRight.hasNext();
    }

    /**
     * Test if any variables in term are in the active set.
     */
    public boolean isActive(ITerm term, @SuppressWarnings("unchecked") D... excludedDeps) {
        Set<D> excludedDepList = Sets.newHashSet(excludedDeps);
        for(ITermVar var : find(term).getVars()) {
            for(D dep : activeVars.get(var)) {
                if(!excludedDepList.contains(dep)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add variables in term to active set.
     */
    public void addActive(ITerm term, D dep) {
        for(ITermVar var : find(term).getVars()) {
            if(frozenVars.contains(var)) {
                throw new IllegalArgumentException("Re-activating frozen " + var);
            }
            activeVars.put(var, dep);
        }
    }

    /**
     * Remove variables in term from active set.
     */
    public void removeActive(ITerm term, D dep) {
        for(ITermVar var : find(term).getVars()) {
            activeVars.remove(var, dep);
        }
    }

    public Set<ITermVar> getActiveVars() {
        return Collections.unmodifiableSet(activeVars.keySet());
    }

    private void updateActive(ITermVar var, ITerm term) {
        if(frozenVars.contains(var)) {
            throw new IllegalArgumentException("Updating frozen " + var);
        }
        if(!activeVars.containsKey(var)) {
            return;
        }
        final Set<D> n = activeVars.get(var);
        for(ITermVar newVar : term.getVars()) {
            if(frozenVars.contains(newVar)) {
                throw new IllegalArgumentException("Re-activating frozen " + newVar + " on update of " + var);
            }
            activeVars.putAll(newVar, n);
        }
        activeVars.removeAll(var);
    }

    public void freeze(ITerm term) {
        for(ITermVar var : find(term).getVars()) {
            if(activeVars.containsKey(var)) {
                throw new IllegalArgumentException("Freezing active " + var);
            }
            frozenVars.add(var);
        }
    }

    /**
     * Add determination
     */
    public void addDetermination(ITerm term, ITerm key, E by) {
        for(ITermVar var : find(term).getVars()) {
            varDetermined.put(var, key, by);
        }
    }

    /**
     * Get objects that determine variables in this term
     */
    public Set<E> isDeterminedBy(ITerm term, ITerm key) {
        final Set<E> bys = Sets.newHashSet();
        for(ITermVar var : term.getVars()) {
            bys.addAll(varDetermined.get(var, key).asSet());
        }
        return bys;
    }

    private void updateDetermination(ITermVar var, ITerm term) {
        for(Entry<ITerm, E> objKey : Sets.newHashSet(varDetermined.get(var))) {
            for(ITermVar newVar : term.getVars()) {
                varDetermined.remove(var, objKey.getKey(), objKey.getValue());
                varDetermined.put(newVar, objKey.getKey(), objKey.getValue());
            }
        }
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + activeVars.hashCode();
        result = prime * result + frozenVars.hashCode();
        result = prime * result + reps.hashCode();
        result = prime * result + sizes.hashCode();
        result = prime * result + varDetermined.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") final Unifier<D, E> other = (Unifier<D, E>) obj;
        if(!activeVars.equals(other.activeVars))
            return false;
        if(!frozenVars.equals(other.frozenVars))
            return false;
        if(!reps.equals(other.reps))
            return false;
        if(!sizes.equals(other.sizes))
            return false;
        if(!varDetermined.equals(other.varDetermined))
            return false;
        return true;
    }
    
}