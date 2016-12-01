package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.equality.Equal;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.equality.Inequal;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFactory;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class EqualitySolver implements ISolverComponent<IEqualityConstraint> {

    private static final long serialVersionUID = 5222408971798066584L;

    private final ITermFactory termFactory;

    private final Set<IEqualityConstraint> defered;

    private final Map<ITermVar,ITerm> reps;
    private final Map<ITermVar,Integer> sizes;

    public EqualitySolver(ITermFactory termFactory) {
        this.termFactory = termFactory;
        this.defered = Sets.newHashSet();
        this.reps = Maps.newHashMap();
        this.sizes = Maps.newHashMap();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(IEqualityConstraint constraint) throws UnsatisfiableException {
        if (!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        Iterator<IEqualityConstraint> it = defered.iterator();
        boolean progress = false;
        while (it.hasNext()) {
            if (solve(it.next())) {
                progress |= true;
                it.remove();
            }
        }
        return progress;
    }

    @Override public void finish() throws UnsatisfiableException {
        for (IEqualityConstraint constraint : defered) {
            throw new UnsatisfiableException(constraint);
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IEqualityConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(Equal constraint) throws UnsatisfiableException {
        unify(constraint.getLeft(), constraint.getRight());
        return true;
    }

    private boolean solve(Inequal constraint) throws UnsatisfiableException {
        ITerm left = find(constraint.getLeft());
        ITerm right = find(constraint.getRight());
        if (left.equals(right)) {
            throw new UnsatisfiableException(constraint);
        }
        return !canUnify(left, right);
    }

    // ------------------------------------------------------------------------------------------------------//

    /**
     * Find representative term.
     */
    public ITerm find(ITerm term) {
        return term.match(Terms.<ITerm> cases(
            // @formatter:off
            appl -> termFactory.newAppl(appl.getOp(), finds(appl.getArgs())),
            tuple -> termFactory.newTuple(finds(tuple.getArgs())),
            ListTerms.cases(
                cons -> termFactory.newCons(find(cons.getHead()), (IListTerm) find(cons.getTail())),
                nil -> nil,
                this::findVarRep
            ),
            string -> string,
            integer -> integer,
            this::findVarRep
            // @formatter:on
        ));
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
        return term.match(Terms.<ITerm> cases()
            // @formatter:off
            .var(this::findVarRep)
            .otherwise(() -> term)
            // @formatter:on
        );
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

    /**
     * Unify two terms.
     * 
     * @return Unified term
     */
    public Unit unify(ITerm left, ITerm right) throws UnsatisfiableException {
        ITerm leftRep = findShallow(left);
        ITerm rightRep = findShallow(right);
        if (leftRep.equals(rightRep)) {
            return unit;
        }
        return leftRep.matchOrThrow(Terms.<Unit, UnsatisfiableException> checkedCases(
            // @formatter:off
            applLeft -> rightRep.matchOrThrow(Terms.<Unit, UnsatisfiableException>checkedCases()
                    .appl(applRight -> {
                        if (!(applLeft.getOp().equals(applRight.getOp()) && applLeft.getArity() == applRight.getArity())) {
                            throw new UnsatisfiableException();
                        }
                        unifys(applLeft.getArgs(), applRight.getArgs());
                        return unit;
                    })
                    .var(var -> unify(var,applLeft))
                    .otherwise(() -> { throw new UnsatisfiableException(); })),
            tupleLeft -> rightRep.matchOrThrow(Terms.<Unit, UnsatisfiableException>checkedCases()
                    .tuple(tupleRight -> {
                        if (tupleLeft.getArity() != tupleRight.getArity()) {
                            throw new UnsatisfiableException();
                        }
                        unifys(tupleLeft.getArgs(), tupleRight.getArgs());
                        return unit;
                    })
                    .var(var -> unify(var,tupleLeft))
                    .otherwise(() -> { throw new UnsatisfiableException(); })),
            listLeft -> rightRep.matchOrThrow(Terms.<Unit, UnsatisfiableException>checkedCases()
                    .list(listRight -> {
                        if (listLeft.getLength() != listRight.getLength()) {
                            throw new UnsatisfiableException();
                        }
                        unifys(listLeft, listRight);
                        return unit;
                    })
                    .var(var -> unify(var,listLeft))
                    .otherwise(() -> { throw new UnsatisfiableException(); })),
            stringLeft -> rightRep.matchOrThrow(Terms.<Unit, UnsatisfiableException>checkedCases()
                    .string(stringRight -> {
                        if(!stringLeft.getValue().equals(stringRight.getValue())) {
                            throw new UnsatisfiableException();
                        }
                        return unit;
                    })
                    .var(var -> unify(var,stringLeft))
                    .otherwise(() -> { throw new UnsatisfiableException(); })),
            integerLeft -> rightRep.matchOrThrow(Terms.<Unit, UnsatisfiableException>checkedCases()
                    .integer(integerRight -> {
                        if(integerLeft.getValue() != integerRight.getValue()) {
                            throw new UnsatisfiableException();
                        }
                        return unit;
                    })
                    .var(var -> unify(var,integerLeft))
                    .otherwise(() -> { throw new UnsatisfiableException(); })),
            varLeft -> rightRep.matchOrThrow(Terms.<Unit, UnsatisfiableException>checkedCases()
                    .var(varRight -> {
                        int sizeLeft = sizes.getOrDefault(varLeft, 1);
                        int sizeRight = sizes.getOrDefault(varRight, 1);
                        if ( sizeLeft > sizeRight) {
                            reps.put(varRight, varLeft);
                            sizes.put(varLeft, sizeLeft + sizeRight);
                        } else {
                            reps.put(varLeft, varRight);
                            sizes.put(varRight, sizeLeft + sizeRight);
                        }
                        return unit;
                    })
                    .otherwise(() -> {
                        reps.put(varLeft, rightRep);
                        return unit;
                    }))
            // @formatter:on
        ));
    }

    private Unit unifys(Iterable<ITerm> lefts, Iterable<ITerm> rights) throws UnsatisfiableException {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while (itLeft.hasNext()) {
            if (!itRight.hasNext()) {
                throw new UnsatisfiableException();
            }
            unify(itLeft.next(), itRight.next());
        }
        if (itRight.hasNext()) {
            throw new UnsatisfiableException();
        }
        return unit;
    }


    private boolean canUnify(ITerm left, ITerm right) {
        return left.match(Terms.<Boolean> cases(
            // @formatter:off
            applLeft -> right.match(Terms.<Boolean> cases()
                    .appl(applRight -> (applLeft.getOp().equals(applRight.getOp()) && (applLeft.getArity() == applLeft.getArity()) && canUnifys(applLeft.getArgs(), applRight.getArgs())))
                    .var(varRight -> true)
                    .otherwise(() -> false)
            ),
            tupleLeft -> right.match(Terms.<Boolean> cases()
                    .tuple(tupleRight -> ((tupleLeft.getArity() == tupleRight.getArity()) && canUnifys(tupleLeft.getArgs(), tupleRight.getArgs())))
                    .var(varRight -> true)
                    .otherwise(() -> false)
            ),
            listLeft -> right.match(Terms.<Boolean> cases()
                    .list(listRight -> (listLeft.getLength() == listRight.getLength()) && canUnifys(listLeft, listRight))
                    .var(varRight -> true)
                    .otherwise(() -> false)
            ),
            stringLeft -> right.match(Terms.<Boolean> cases()
                    .string(stringRight -> stringLeft.getValue().equals(stringRight.getValue()))
                    .var(varRight -> true)
                    .otherwise(() -> false)
            ),
            integerLeft -> right.match(Terms.<Boolean> cases()
                    .integer(integerRight -> (integerLeft.getValue() == integerRight.getValue()))
                    .var(varRight -> false)
                    .otherwise(() -> false)),
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