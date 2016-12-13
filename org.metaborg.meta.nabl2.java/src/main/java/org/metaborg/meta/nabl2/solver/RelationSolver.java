package org.metaborg.meta.nabl2.solver;

import java.util.Iterator;
import java.util.Set;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.relations.CBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.CCheckRelation;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint.CheckedCases;
import org.metaborg.meta.nabl2.relations.Relation;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.CM;
import org.metaborg.meta.nabl2.unification.Unifier;

import com.google.common.collect.Sets;

public class RelationSolver implements ISolverComponent<IRelationConstraint> {

    private final Unifier unifier;
    private final Relations relations;

    private final Set<IRelationConstraint> defered = Sets.newHashSet();

    public RelationSolver(Relations relations, Unifier unifier) {
        this.unifier = unifier;
        this.relations = relations;
    }

    public IRelations getRelations() {
        return relations;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(IRelationConstraint constraint) throws UnsatisfiableException {
        if (!solve(constraint)) {
            defered.add(constraint);
        }
        return Unit.unit;

    }

    @Override public boolean iterate() throws UnsatisfiableException {
        Iterator<IRelationConstraint> it = defered.iterator();
        boolean progress = false;
        while (it.hasNext()) {
            try {
                if (solve(it.next())) {
                    progress = true;
                    it.remove();
                }
            } catch (UnsatisfiableException e) {
                progress = true;
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    @Override public void finish() throws UnsatisfiableException {
        if (!defered.isEmpty()) {
            throw new UnsatisfiableException("Unsolved relation constraint.", defered.toArray(new IConstraint[0]));
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IRelationConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(CBuildRelation c) throws UnsatisfiableException {
        ITerm left = unifier.find(c.getLeft());
        if (!left.isGround()) {
            return false;
        }
        ITerm right = unifier.find(c.getRight());
        if (!right.isGround()) {
            return false;
        }
        try {
            relations.getRelation(c.getRelation()).add(left, right);
        } catch (RelationException e) {
            throw new UnsatisfiableException(e, c);
        }
        return true;
    }

    private boolean solve(CCheckRelation c) throws UnsatisfiableException {
        ITerm left = unifier.find(c.getLeft());
        ITerm right = unifier.find(c.getRight());
        if (!(left.isGround() && right.isGround())) {
            return false;
        }
        Relation<ITerm> relation = relations.getRelation(c.getRelation());
        return CM.list((leftList) -> {
            return CM.list((rightList) -> {
                if (leftList.getLength() != rightList.getLength()) {
                    throw new UnsatisfiableException("Lists have different length", c);
                }
                Iterator<ITerm> leftIt = leftList.iterator();
                Iterator<ITerm> rightIt = rightList.iterator();
                while (leftIt.hasNext()) {
                    if (!relation.contains(leftIt.next(), rightIt.next())) {
                        return false;
                    }
                }
                return true;
            }).matchOrThrow(right).orElseThrow(() -> new UnsatisfiableException(c));
        }).matchOrThrow(left).orElseGet(() -> relation.contains(left, right));
    }

    // ------------------------------------------------------------------------------------------------------//

}