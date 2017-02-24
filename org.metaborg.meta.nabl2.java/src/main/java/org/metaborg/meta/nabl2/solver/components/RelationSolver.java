package org.metaborg.meta.nabl2.solver.components;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
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
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class RelationSolver extends SolverComponent<IRelationConstraint> {

    private final Relations<ITerm> relations;
    private final Map<String, Function1<ITerm, Optional<ITerm>>> functions;

    private final Multimap<IRelationName, IRelationConstraint> deferedBuilds = HashMultimap.create();
    private final Set<IRelationConstraint> deferedChecks = Sets.newHashSet();
    private boolean complete = false;

    public RelationSolver(Solver solver, Relations<ITerm> relations,
        Map<String, Function1<ITerm, Optional<ITerm>>> functions) {
        super(solver);
        this.relations = relations;
        this.functions = Maps.newHashMap(functions);
        addRelationFunctions();
    }

    private void addRelationFunctions() {
        for(IRelationName relationName : relations.getNames()) {
            String lubName = RelationTerms.relationFunction(relationName, RelationFunctions.LUB);
            Function1<ITerm, Optional<ITerm>> lubFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return lub(relationName, left, right);
            }))::match;
            functions.put(lubName, lubFun);
            String glbName = RelationTerms.relationFunction(relationName, RelationFunctions.GLB);
            Function1<ITerm, Optional<ITerm>> glbFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return glb(relationName, left, right);
            }))::match;
            functions.put(glbName, glbFun);
        }
    }

    public IRelations<ITerm> getRelations() {
        return relations;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(IRelationConstraint constraint) throws UnsatisfiableException {
        if(complete) {
            throw new IllegalStateException("Cannot add constraints after iteration started.");
        }
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add, this::add));
    }

    private Unit add(CBuildRelation constraint) throws UnsatisfiableException {
        if(isPartial() || !solve(constraint)) {
            deferedBuilds.put(constraint.getRelation(), constraint);
        }
        return Unit.unit;

    }

    private Unit add(CCheckRelation constraint) throws UnsatisfiableException {
        if(isPartial() || !solve(constraint)) {
            deferedChecks.add(constraint);
        }
        return Unit.unit;

    }

    private Unit add(CEvalFunction constraint) throws UnsatisfiableException {
        unifier().addActive(constraint.getResult());
        if(isPartial() || !solve(constraint)) {
            deferedChecks.add(constraint);
        }
        return Unit.unit;

    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        if(isPartial()) {
            return false;
        }
        complete = true;
        boolean progress = false;
        progress |= doIterate(deferedBuilds.values(), this::solve);
        progress |= doIterate(deferedChecks, this::solve);
        return progress;
    }

    @Override protected Iterable<IRelationConstraint> doFinish(IMessageInfo messageInfo) {
        return Iterables2.fromConcat(deferedBuilds.values(), deferedChecks);
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IRelationConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve, this::solve));
    }

    private boolean solve(CBuildRelation c) throws UnsatisfiableException {
        ITerm left = unifier().find(c.getLeft());
        if(!left.isGround()) {
            return false;
        }
        ITerm right = unifier().find(c.getRight());
        if(!right.isGround()) {
            return false;
        }
        try {
            relations.add(c.getRelation(), left, right);
        } catch(RelationException e) {
            throw new UnsatisfiableException(c.getMessageInfo().withDefault(MessageContent.of(e.getMessage())));
        }
        return true;
    }

    private boolean solve(CCheckRelation c) throws UnsatisfiableException {
        if(!isComplete(c.getRelation())) {
            return false;
        }
        ITerm left = unifier().find(c.getLeft());
        ITerm right = unifier().find(c.getRight());
        if(!(left.isGround() && right.isGround())) {
            return false;
        }
        return relations.contains(c.getRelation(), left, right);
    }

    private boolean solve(CEvalFunction c) throws UnsatisfiableException {
        ITerm term = unifier().find(c.getTerm());
        if(!term.isGround()) {
            return false;
        }
        Function1<ITerm, Optional<ITerm>> fun = functions.get(c.getFunction());
        if(fun == null) {
            throw new UnsatisfiableException(
                c.getMessageInfo().withDefault(MessageContent.of("Function " + c.getFunction() + " undefined.")));
        }
        Optional<ITerm> result = fun.apply(term);
        if(!result.isPresent()) {
            return false;
        }
        try {
            unifier().removeActive(c.getResult());
            unifier().unify(c.getResult(), result.get());
        } catch(UnificationException ex) {
            throw new UnsatisfiableException(c.getMessageInfo().withDefault(ex.getMessageContent()));
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean isComplete(IRelationName name) {
        return complete && !deferedBuilds.containsKey(name);
    }

    private Optional<ITerm> lub(IRelationName name, ITerm left, ITerm right) {
        return isComplete(name) ? relations.leastUpperBound(name, left, right) : Optional.empty();
    }

    private Optional<ITerm> glb(IRelationName name, ITerm left, ITerm right) {
        return isComplete(name) ? relations.greatestLowerBound(name, left, right) : Optional.empty();
    }

}