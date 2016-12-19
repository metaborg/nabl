package org.metaborg.meta.nabl2.solver;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.relations.CBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.CCheckRelation;
import org.metaborg.meta.nabl2.constraints.relations.CEvalFunction;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint.CheckedCases;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms.RelationFunctions;
import org.metaborg.meta.nabl2.relations.terms.Relations;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class RelationSolver implements ISolverComponent<IRelationConstraint> {

    private final Unifier unifier;
    private final Relations<ITerm> relations;
    private final Map<String,Function1<ITerm,Optional<ITerm>>> functions;

    private final Set<IRelationConstraint> defered = Sets.newHashSet();

    /**************************************************************
     * Least upper bound calculations can be unstable if
     * there are still relation building * constraints unsolved!
     * 
     * @param functions2
     **************************************************************/

    public RelationSolver(Relations<ITerm> relations, Map<String,Function1<ITerm,Optional<ITerm>>> functions,
            Unifier unifier) {
        this.unifier = unifier;
        this.relations = relations;
        this.functions = Maps.newHashMap(functions);
        addRelationFunctions();
    }

    private void addRelationFunctions() {
        for (IRelationName relationName : relations.getNames()) {
            String lubName = RelationTerms.relationFunction(relationName, RelationFunctions.LUB);
            Function1<ITerm,Optional<ITerm>> lubFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return relations.leastUpperBound(relationName, left, right);
            }))::match;
            functions.put(lubName, lubFun);
            String glbName = RelationTerms.relationFunction(relationName, RelationFunctions.GLB);
            Function1<ITerm,Optional<ITerm>> glbFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return relations.greatestLowerBound(relationName, left, right);
            }))::match;
            functions.put(glbName, glbFun);
        }
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
            return c.getMessageInfo().makeException("Unsolved relation constraint: " + c, Iterables2.empty(), unifier);
        }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IRelationConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve));
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
            throw c.getMessageInfo().makeException(e.getMessage(), Iterables2.empty(), unifier);
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

    private boolean solve(CEvalFunction c) throws UnsatisfiableException {
        ITerm term = unifier.find(c.getTerm());
        if (!term.isGround()) {
            return false;
        }
        Function1<ITerm,Optional<ITerm>> fun = functions.get(c.getFunction());
        if (fun == null) {
            throw c.getMessageInfo().makeException("Function " + c.getFunction() + " undefined.", Iterables2.empty(),
                    unifier);
        }
        Optional<ITerm> result = fun.apply(term);
        if (!result.isPresent()) {
            return false;
        }
        try {
            unifier.unify(c.getResult(), result.get());
        } catch (UnificationException ex) {
            throw c.getMessageInfo().makeException(ex.getMessage(), Iterables2.empty(), unifier);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

}