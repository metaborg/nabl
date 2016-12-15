package org.metaborg.meta.nabl2.solver;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.relations.CBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.CCheckRelation;
import org.metaborg.meta.nabl2.constraints.relations.CGlb;
import org.metaborg.meta.nabl2.constraints.relations.CLub;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint.CheckedCases;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.terms.Relations;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Sets;

public class RelationSolver implements ISolverComponent<IRelationConstraint> {

    private final Unifier unifier;
    private final Relations<ITerm> relations;

    private final Set<IRelationConstraint> defered = Sets.newHashSet();

    public RelationSolver(Relations<ITerm> relations, Unifier unifier) {
        this.unifier = unifier;
        this.relations = relations;
    }

    public IRelations<ITerm> getRelations() {
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

    @Override public Iterable<UnsatisfiableException> finish() {
        return defered.stream().map(c -> {
            return c.getMessageInfo().makeException("Unsolved relation constraint: " + c, Iterables2.empty());
        }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IRelationConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve, this::solve));
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
            relations.add(c.getRelation(), left, right);
        } catch (RelationException e) {
            throw c.getMessageInfo().makeException(e.getMessage(), Iterables2.empty());
        }
        return true;
    }

    private boolean solve(CCheckRelation c) throws UnsatisfiableException {
        ITerm left = unifier.find(c.getLeft());
        ITerm right = unifier.find(c.getRight());
        if (!(left.isGround() && right.isGround())) {
            return false;
        }
        return relations.contains(c.getRelation(), left, right);
    }

    private boolean solve(CLub c) throws UnsatisfiableException {
        ITerm left = unifier.find(c.getLeft());
        ITerm right = unifier.find(c.getRight());
        if (!(left.isGround() && right.isGround())) {
            return false;
        }

        Optional<ITerm> lub = relations.leastUpperBound(c.getRelation(), left, right);
        if (!lub.isPresent()) {
            return false;
        }
        try {
            unifier.unify(c.getResult(), lub.get());
        } catch (UnificationException ex) {
            throw c.getMessageInfo().makeException(ex.getMessage(), Iterables2.empty());
        }
        return true;
    }

    private boolean solve(CGlb c) throws UnsatisfiableException {
        ITerm left = unifier.find(c.getLeft());
        ITerm right = unifier.find(c.getRight());
        if (!(left.isGround() && right.isGround())) {
            return false;
        }
        Optional<ITerm> glb = relations.greatestLowerBound(c.getRelation(), left, right);
        if (!glb.isPresent()) {
            return false;
        }
        try {
            unifier.unify(c.getResult(), glb.get());
        } catch (UnificationException ex) {
            throw c.getMessageInfo().makeException(ex.getMessage(), Iterables2.empty());
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

}