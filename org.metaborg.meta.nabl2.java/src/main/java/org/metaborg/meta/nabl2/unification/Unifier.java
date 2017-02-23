package org.metaborg.meta.nabl2.unification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

public class Unifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<ITermVar,ITerm> reps;
    private final Map<ITermVar,Integer> sizes;
    private final Multiset<ITermVar> activeVars;

    public Unifier() {
        this.reps = Maps.newHashMap();
        this.sizes = Maps.newHashMap();
        this.activeVars = HashMultiset.create();
    }

    @Override public Iterable<ITermVar> getAllVars() {
        return Collections.unmodifiableSet(reps.keySet());
    }
    
    /**
     * Find representative term.
     */
    public ITerm find(ITerm term) {
        return term.match(Terms.<ITerm>casesFix(
            // @formatter:off
            (tf, appl) -> GenericTerms.newAppl(appl.getOp(), finds(appl.getArgs()), appl.getAttachments()),
            (tf, list) -> list.match(ListTerms.<ITerm>casesFix(
                (lf, cons) -> GenericTerms.newCons(cons.getHead().match(tf), (IListTerm) cons.getTail().match(lf), cons.getAttachments()),
                (lf, nil) -> nil,
                (lf, var) -> findVarRep(var)
            )),
            (tf, string) -> string,
            (tf, integer) -> integer,
            (tf, var) -> findVarRep(var)
            // @formatter:on
        ));
    }

    private ITerm findVarRep(ITermVar var) {
        if (!reps.containsKey(var)) {
            return var;
        } else {
            ITerm rep = find(reps.get(var));
            reps.put(var, rep);
            return rep;
        }
    }

    private Iterable<ITerm> finds(Iterable<ITerm> terms) {
        List<ITerm> reps = Lists.newArrayList();
        for (ITerm term : terms) {
            reps.add(find(term));
        }
        return reps;
    }

    /**
     * Find representative term, without recursing on subterms.
     */
    private ITerm findShallow(ITerm term) {
        return M.var(this::findVarRepShallow).match(term).orElse(term);
    }

    private ITerm findVarRepShallow(ITermVar var) {
        if (!reps.containsKey(var)) {
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
        if (!unifyTerms(left, right)) {
            throw new UnificationException(find(left), find(right));
        }
    }

    private boolean unifyTerms(ITerm left, ITerm right) {
        ITerm leftRep = findShallow(left);
        ITerm rightRep = findShallow(right);
        if (leftRep.termEquals(rightRep)) {
            return true;
        } else if (leftRep.isGround() && rightRep.isGround()) {
            return false;
        }
        return leftRep.match(Terms.<Boolean> cases(
            // @formatter:off
            applLeft -> M.<Boolean>cases(
                M.appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                    applLeft.getArity() == applRight.getArity() &&
                                    unifys(applLeft.getArgs(), applRight.getArgs())),
                M.var(varRight -> unifyVarTerm(varRight, applLeft))
            ).match(rightRep).orElse(false),
            listLeft -> M.<Boolean>cases(
                M.list(listRight -> unifyLists(listLeft, listRight)),
                M.var(varRight -> unifyTerms(varRight, listLeft))
            ).match(rightRep).orElse(false),
            stringLeft -> M.<Boolean>cases(
                M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                M.var(varRight -> unifyVarTerm(varRight, stringLeft))
            ).match(rightRep).orElse(false),
            integerLeft -> M.<Boolean>cases(
                M.integer(integerRight -> integerLeft.getValue() != integerRight.getValue()),
                M.var(varRight -> unifyVarTerm(varRight, integerLeft))
            ).match(rightRep).orElse(false),
            varLeft -> M.cases(
                M.var(varRight -> unifyVars(varLeft, varRight)),
                M.term(termRight -> unifyVarTerm(varLeft, termRight))
            ).match(rightRep).orElse(false)
            // @formatter:on
        ));
    }

    public boolean unifyLists(IListTerm left, IListTerm right) {
        return left.match(ListTerms.<Boolean> cases(
            // @formatter:off
            consLeft -> M.<Boolean> cases(
                M.cons(consRight -> {
                    return unifyTerms(consLeft.getHead(), consRight.getHead()) &&
                           unifyLists(consLeft.getTail(), consRight.getTail());
                }),
                M.var(varRight -> unifyVarTerm(varRight, consLeft))
            ).match(right).orElse(false),
            nilLeft -> M.<Boolean> cases(
                M.nil(nilRight -> true),
                M.var(varRight -> unifyVarTerm(varRight, nilLeft))
            ).match(right).orElse(false),
            varLeft -> M.<Boolean> cases(
                M.var(varRight -> unifyVars(varLeft, varRight)),
                M.term(termRight -> unifyVarTerm(varLeft, termRight))
            ).match(right).orElse(false)
            // @formatter:on
        ));
    }

    private boolean unifyVarTerm(ITermVar var, ITerm term) {
        if (term.getVars().contains(var)) {
            return false;
        }
        reps.put(var, term);
        updateActive(var, term);
        return true;
    }

    private boolean unifyVars(ITermVar varLeft, ITermVar varRight) {
        int sizeLeft = sizes.getOrDefault(varLeft, 1);
        int sizeRight = sizes.getOrDefault(varRight, 1);
        if (sizeLeft > sizeRight) {
            reps.put(varRight, varLeft);
            sizes.put(varLeft, sizeLeft + sizeRight);
            updateActive(varRight, varLeft);
        } else {
            reps.put(varLeft, varRight);
            sizes.put(varRight, sizeLeft + sizeRight);
            updateActive(varLeft, varRight);
        }
        return true;
    }

    private boolean unifys(Iterable<ITerm> lefts, Iterable<ITerm> rights) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        boolean success = true;
        while (itLeft.hasNext()) {
            if (!itRight.hasNext()) {
                return false;
            }
            success &= unifyTerms(itLeft.next(), itRight.next());
        }
        if (itRight.hasNext()) {
            return false;
        }
        return success;
    }


    public boolean canUnify(ITerm left, ITerm right) {
        return left.match(Terms.<Boolean> cases(
            // @formatter:off
            applLeft -> M.<Boolean>cases(
                M.appl(applRight -> (applLeft.getOp().equals(applRight.getOp()) &&
                                     applLeft.getArity() == applLeft.getArity() &&
                                     canUnifys(applLeft.getArgs(), applRight.getArgs()))),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            listLeft -> M.<Boolean>cases(
                M.list(listRight -> canUnifyLists(listLeft, listRight)),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            stringLeft -> M.<Boolean>cases(
                M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            integerLeft -> M.<Boolean>cases(
                M.integer(integerRight -> (integerLeft.getValue() == integerRight.getValue())),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            varLeft -> true
            // @formatter:on
        ));
    }

    public boolean canUnifyLists(IListTerm left, IListTerm right) {
        return left.match(ListTerms.<Boolean> cases(
            // @formatter:off
            consLeft -> M.<Boolean>cases(
                M.cons(consRight -> (canUnify(consLeft.getHead(), consRight.getHead()) &&
                                     canUnifyLists(consLeft.getTail(), consRight.getTail()))),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            nilLeft -> M.<Boolean>cases(
                M.nil(nilRight -> true),
                M.var(varRight -> true)
            ).match(right).orElse(false),
            varLeft -> true
            // @formatter:on
        ));

    }

    private boolean canUnifys(Iterable<ITerm> lefts, Iterable<ITerm> rights) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while (itLeft.hasNext()) {
            if (!(itRight.hasNext() && canUnify(itLeft.next(), itRight.next()))) {
                return false;
            }
        }
        return !itRight.hasNext();
    }

    /**
     * Test if any variables in term are in the active set.
     */
    public boolean isActive(ITerm term) {
        for (ITermVar var : find(term).getVars()) {
            if (activeVars.contains(var)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add variables in term to active set.
     */
    public void addActive(ITerm term) {
        for (ITermVar var : find(term).getVars()) {
            activeVars.add(var);
        }
    }
    
    /**
     * Remove variables in term from active set.
     */
    public void removeActive(ITerm term) {
        for (ITermVar var : find(term).getVars()) {
            activeVars.remove(var);
        }
    }
 
    public Iterable<ITermVar> getActiveVars() {
        return Collections.unmodifiableSet(activeVars.elementSet());
    }
    
    private void updateActive(ITermVar var, ITerm term) {
        if (!activeVars.contains(var)) {
            //return;
        }
        int n = activeVars.count(var);
        for (ITermVar v : term.getVars()) {
            activeVars.add(v, n);
        }
        activeVars.remove(var, n);
    }
    
}