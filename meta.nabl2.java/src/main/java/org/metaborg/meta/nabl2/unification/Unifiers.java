package org.metaborg.meta.nabl2.unification;

import java.util.Iterator;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;

public class Unifiers {

    public static boolean canUnify(ITerm left, ITerm right) {
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

    private static boolean canUnifys(Iterable<ITerm> lefts, Iterable<ITerm> rights) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!(itRight.hasNext() && canUnify(itLeft.next(), itRight.next()))) {
                return false;
            }
        }
        return !itRight.hasNext();
    }

}