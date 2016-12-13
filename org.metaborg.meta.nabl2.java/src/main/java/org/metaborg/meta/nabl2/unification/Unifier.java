package org.metaborg.meta.nabl2.unification;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.CM;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Unifier implements IUnifier, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<ITermVar,ITerm> reps;
    private final Map<ITermVar,Integer> sizes;

    public Unifier() {
        this.reps = Maps.newHashMap();
        this.sizes = Maps.newHashMap();
    }

    /**
     * Find representative term.
     */
    public ITerm find(ITerm term) {
        return term.match(Terms.<ITerm> cases(
            // @formatter:off
            appl -> GenericTerms.newAppl(appl.getOp(), finds(appl.getArgs())),
            list -> list.match(ListTerms.<ITerm>cases(
                cons -> GenericTerms.newCons(find(cons.getHead()), (IListTerm) find(cons.getTail())),
                nil -> nil,
                this::findVarRep)),
            string -> string,
            integer -> integer,
            this::findVarRep
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
    public Unit unify(ITerm left, ITerm right) throws UnificationException {
        ITerm leftRep = findShallow(left);
        ITerm rightRep = findShallow(right);
        if (leftRep.equals(rightRep)) {
            return unit;
        } else if (leftRep.isGround() && rightRep.isGround()) {
            throw new UnificationException("Cannot unify different ground terms.");
        }
        return leftRep.matchOrThrow(Terms.<Unit, UnificationException> checkedCases(
            // @formatter:off
            applLeft -> CM.cases(
                CM.appl(applRight -> {
                    if(!(applLeft.getOp().equals(applRight.getOp()) && applLeft.getArity() == applRight.getArity())) {
                        throw new UnificationException("Cannot unify different term constructors.");
                    }
                    unifys(applLeft.getArgs(), applRight.getArgs());
                    return unit;
                }),
                CM.var(varRight -> unify(varRight, applLeft))
            ).matchOrThrow(rightRep).orElseThrow(() -> new UnificationException("Failed")),
            listLeft -> CM.cases(
                CM.list(listRight -> {
                    if(listLeft.getLength() != listRight.getLength()) {
                        throw new UnificationException("Cannot unify lists of different length.");
                    }
                    unifys(listLeft, listRight);
                    return unit;
                }),
                CM.var(varRight -> unify(varRight, listLeft))
            ).matchOrThrow(rightRep).orElseThrow(() -> new UnificationException("Failed")),
            stringLeft -> CM.cases(
                CM.string(stringRight -> {
                    if(!stringLeft.getValue().equals(stringRight.getValue())) {
                        throw new UnificationException("Cannot unify different strings.");
                    }
                    return unit;
                }),
                CM.var(varRight -> unify(varRight, stringLeft))
            ).matchOrThrow(rightRep).orElseThrow(() -> new UnificationException("Failed")),
            integerLeft -> CM.cases(
                CM.integer(integerRight -> {
                    if(integerLeft.getValue() != integerRight.getValue()) {
                        throw new UnificationException("Cannot unify different integers.");
                    }
                    return unit;
                }),
                CM.var(varRight -> unify(varRight, integerLeft))
            ).matchOrThrow(rightRep).orElseThrow(() -> new UnificationException("Failed")),
            varLeft -> M.cases(
                M.var(varRight -> {
                    int sizeLeft = sizes.getOrDefault(varLeft, 1);
                    int sizeRight = sizes.getOrDefault(varRight, 1);
                    if(sizeLeft > sizeRight) {
                        reps.put(varRight, varLeft);
                        sizes.put(varLeft, sizeLeft + sizeRight);
                    } else {
                        reps.put(varLeft, varRight);
                        sizes.put(varRight, sizeLeft + sizeRight);
                    }
                    return unit;
                }),
                M.term(termRight -> {
                    reps.put(varLeft, termRight);
                    return unit;
                })
            ).match(rightRep).orElseThrow(() -> new IllegalStateException())
            // @formatter:on
        ));
    }

    private Unit unifys(Iterable<ITerm> lefts, Iterable<ITerm> rights) throws UnificationException {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while (itLeft.hasNext()) {
            if (!itRight.hasNext()) {
                throw new UnificationException("Cannot unify different number of arguments.");
            }
            unify(itLeft.next(), itRight.next());
        }
        if (itRight.hasNext()) {
            throw new UnificationException("Cannot unify different number of arguments.");
        }
        return unit;
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
                            M.list(listRight -> (listLeft.getLength() == listRight.getLength())),
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


}