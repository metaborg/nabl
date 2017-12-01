package org.metaborg.meta.nabl2.unification;

import java.util.Iterator;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;

// TODO: Fix interface, locking, and matching.
// 1) Decide what the public interface should be. It should not return variables
//    that have a different class representative, but it may return variables that
//    have a term, to allow lazy matching. So a shallow find, a deep find (but this
//    shouldn't be used really). Maybe also a method to get the class representative
//    of a variable, to base an entailment checking algorithm on.
// 2) Restore support for locking. Where can locked terms appear? Not in the domain,
//    but probably in the co-domain. This might hurt performance a bit, because tree
//    balancing is not possible when one of the variables is locked. Also think about
//    what is returned when finding a (partially) locked term; should the lock of the
//    input term be preserved, or should we take the union of the input and the found
//    term?
// 3) The default find should be shallow, which means the matching code must be changed
//    to support lazy matching. Basically, we only traverse terms for as much as required
//    to do the match.

// IDEA: Unify unification, locking, and matching.
// Unification with a locked term and matching are very similar. We will need matching
// against patterns to support user-defined rules. Of course we can also translate match
// patterns to Matcher<ITerm>s, but it feels like these matchers duplicate some of the
// unification code.

public class FastUnifier {

    private final Map.Transient<ITermVar, ITermVar> reps;
    private final Map.Transient<ITermVar, Integer> sizes;
    private final Map.Transient<ITermVar, ITerm> terms;

    public FastUnifier() {
        this.reps = Map.Transient.of();
        this.sizes = Map.Transient.of();
        this.terms = Map.Transient.of();
    }

    public Map.Immutable<ITermVar, ITerm> sub() {
        final Map.Transient<ITermVar, ITerm> sub = Map.Transient.of();
        sub.__putAll(reps);
        sub.__putAll(terms);
        return sub.freeze();
    }

    /**
     * Find the class representative for the variable.
     */
    private ITermVar findRep(ITermVar var) {
        if(!reps.containsKey(var)) {
            return var;
        } else {
            ITermVar rep = findRep(reps.get(var));
            reps.__put(var, rep);
            return rep;
        }
    }

    /**
     * Find the class term for the variable. The variable does not have to be the class representative. If a term is
     * associated with the class, all variables in it are substituted by their class representatives (but not by the
     * class terms). If no term is associated with the class, the class representative is returned.
     */
    private ITerm findTerm(ITermVar var) {
        final ITermVar rep = findRep(var);
        if(!terms.containsKey(rep)) {
            return rep;
        } else {
            ITerm term = terms.getOrDefault(rep, rep);
            term = M.somebu(M.var(this::findRep)).apply(term);
            return term;
        }
    }

    /**
     * Unify two terms.
     * 
     * @return Unified term
     */
    public UnificationResult unify(ITerm left, ITerm right) throws UnificationException {
        final UnificationResult result = new UnificationResult();
        if(!unifyTerms(left, right, result)) {
            throw new UnificationException(left, right);
        }
        return result;
    }

    private boolean unifyTerms(ITerm left, ITerm right, UnificationResult result) {
        // @formatter:off
        return left.match(Terms.cases(
            applLeft -> M.cases(
                M.appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                    applLeft.getArity() == applRight.getArity() &&
                                    unifys(applLeft.getArgs(), applRight.getArgs(), result)),
                M.var(varRight -> unifyVarTerm(varRight, applLeft, result))
            ).match(right).orElse(false),
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
            ).match(right).orElse(false),
            stringLeft -> M.cases(
                M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                M.var(varRight -> unifyVarTerm(varRight, stringLeft, result))
            ).match(right).orElse(false),
            integerLeft -> M.cases(
                M.integer(integerRight -> integerLeft.getValue() != integerRight.getValue()),
                M.var(varRight -> unifyVarTerm(varRight, integerLeft, result))
            ).match(right).orElse(false),
            blobLeft -> M.cases(
                M.blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue())),
                M.var(varRight -> unifyVarTerm(varRight, blobLeft, result))
            ).match(right).orElse(false),
            varLeft -> M.cases(
                // match var before term, or term will always match
                M.var(varRight -> unifyVars(varLeft, varRight, result)),
                M.term(termRight -> unifyVarTerm(varLeft, termRight, result))
            ).match(right).orElse(false)
        ));
        // @formatter:on
    }

    private boolean unifyVarTerm(ITermVar var, ITerm term, UnificationResult result) {
        final ITermVar rep = findRep(var);
        final boolean res;
        if(terms.containsKey(rep)) {
            res = unifyTerms(findTerm(rep), term, result);
        } else {
            terms.put(var, term);
            result.addSubstituted(var);
            res = true;
        }
        return res && !hasCycle(rep);
    }

    private boolean unifyVars(ITermVar left, ITermVar right, UnificationResult result) {
        final ITermVar leftRep = findRep(left).withLocked(left.isLocked());
        final ITermVar rightRep = findRep(right).withLocked(right.isLocked());
        if(leftRep.equals(rightRep)) {
            return true;
        }
        final int sizeLeft = sizes.getOrDefault(leftRep, 1);
        final int sizeRight = sizes.getOrDefault(rightRep, 1);
        final boolean swap = sizeLeft > sizeRight;
        final ITermVar var = swap ? rightRep : leftRep; // the eliminated variable
        final ITermVar with = swap ? leftRep : rightRep; // the new representative
        sizes.put(with, sizeLeft + sizeRight);
        reps.put(var, with);
        result.addSubstituted(var);
        final ITerm term = terms.__remove(var);
        if(term != null) {
            return unifyTerms(findTerm(var), findTerm(with), result);
        } else {
            return true;
        }
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

    private boolean hasCycle(final ITermVar var) {
        return hasCycle(var, Sets.newHashSet(), Sets.newHashSet());
    }

    private boolean hasCycle(final ITermVar var, final Set<ITermVar> stack, final Set<ITermVar> visited) {
        final ITermVar rep = findRep(var);
        visited.add(rep);
        stack.add(rep);
        final ITerm term = terms.get(rep);
        if(term != null) {
            for(ITermVar next : term.getVars()) {
                if(!visited.contains(next)) {
                    if(hasCycle(next, stack, visited)) {
                        return true;
                    }
                } else if(stack.contains(next)) {
                    return true;
                }
            }
        }
        stack.remove(var);
        return false;
    }

}
