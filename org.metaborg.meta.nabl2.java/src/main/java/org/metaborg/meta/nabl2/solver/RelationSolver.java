package org.metaborg.meta.nabl2.solver;

import java.util.Iterator;
import java.util.Map;
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
import org.metaborg.meta.nabl2.relations.Bounds;
import org.metaborg.meta.nabl2.relations.Relation;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.CM;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class RelationSolver implements ISolverComponent<IRelationConstraint> {

    private final Unifier unifier;
    private final Relations relations;

    private final Map<RelationName,Bounds<ITerm>> boundsCache;

    private final Set<IRelationConstraint> defered = Sets.newHashSet();

    public RelationSolver(Relations relations, Unifier unifier) {
        this.unifier = unifier;
        this.relations = relations;
        this.boundsCache = Maps.newHashMap();
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
            relations.getRelation(c.getRelation()).add(left, right);
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
        Relation<ITerm> relation = relations.getRelation(c.getRelation());
        return CM.list((leftList) -> {
            return CM.list((rightList) -> {
                if (leftList.getLength() != rightList.getLength()) {
                    throw c.getMessageInfo().makeException("Lists have different length", Iterables2.empty());
                }
                Iterator<ITerm> leftIt = leftList.iterator();
                Iterator<ITerm> rightIt = rightList.iterator();
                while (leftIt.hasNext()) {
                    if (!relation.contains(leftIt.next(), rightIt.next())) {
                        return false;
                    }
                }
                return true;
            }).matchOrThrow(right).orElseThrow(() -> c.getMessageInfo().makeException("Lists must match another list.",
                    Iterables2.empty()));
        }).matchOrThrow(left).orElseGet(() -> relation.contains(left, right));
    }

    private boolean solve(CLub c) throws UnsatisfiableException {
        ITerm left = unifier.find(c.getLeft());
        ITerm right = unifier.find(c.getRight());
        if (!(left.isGround() && right.isGround())) {
            return false;
        }
        Bounds<ITerm> b = boundsCache.computeIfAbsent(c.getRelation(), r -> new Bounds<>(relations.getRelation(c
                .getRelation())));
        Optional<ITerm> lub = b.leastUpperBound(left, right);
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
        Bounds<ITerm> b = boundsCache.computeIfAbsent(c.getRelation(), r -> new Bounds<>(relations.getRelation(c
                .getRelation())));
        Optional<ITerm> lub = b.greatestLowerbound(left, right);
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

    // ------------------------------------------------------------------------------------------------------//

}