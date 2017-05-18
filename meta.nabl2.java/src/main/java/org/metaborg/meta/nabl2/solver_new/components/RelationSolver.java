package org.metaborg.meta.nabl2.solver_new.components;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.relations.CBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.CCheckRelation;
import org.metaborg.meta.nabl2.constraints.relations.CEvalFunction;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms.RelationFunctions;
import org.metaborg.meta.nabl2.solver.FunctionUndefinedException;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.functions.PartialFunction1;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;

public class RelationSolver extends ASolver<IRelationConstraint, RelationSolver.RelationResult> {

    private final IRelations.Transient<ITerm> relations;
    private final Map.Transient<String, PartialFunction1<ITerm, ITerm>> functions;

    private final Multimap<IRelationName, IRelationConstraint> deferedBuilds = HashMultimap.create();
    private final java.util.Set<IRelationConstraint> deferedChecks = Sets.newHashSet();
    private boolean complete = false;

    public RelationSolver(SolverCore core, IRelations.Immutable<ITerm> relations,
            Map.Immutable<String, PartialFunction1<ITerm, ITerm>> functions) {
        super(core);
        this.relations = relations.melt();
        this.functions = functions.asTransient();
        addRelationFunctions();
    }

    private void addRelationFunctions() {
        for(IRelationName relationName : relations.getNames()) {
            String lubName = RelationTerms.relationFunction(relationName, RelationFunctions.LUB);
            PartialFunction1<ITerm, ITerm> lubFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return lub(relationName, left, right);
            }))::match;
            functions.__put(lubName, lubFun);
            String glbName = RelationTerms.relationFunction(relationName, RelationFunctions.GLB);
            PartialFunction1<ITerm, ITerm> glbFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return glb(relationName, left, right);
            }))::match;
            functions.__put(glbName, glbFun);
        }
    }

    @Override public boolean add(IRelationConstraint constraint) throws InterruptedException {
        return constraint.match(IRelationConstraint.Cases.of(this::add, this::add, this::add));
    }

    @Override public boolean iterate() throws InterruptedException {
        complete = true;
        boolean progress = false;
        progress |= doIterate(deferedBuilds.values(), this::solve);
        progress |= doIterate(deferedChecks, this::solve);
        return progress;
    }

    public RelationResult finish() {
        java.util.Set<IRelationConstraint> constraints = Sets.newHashSet();
        constraints.addAll(deferedBuilds.values());
        constraints.addAll(deferedChecks);
        return ImmutableRelationResult.of(relations.freeze(), constraints);
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean add(CBuildRelation constraint) {
        if(!solve(constraint)) {
            return deferedBuilds.put(constraint.getRelation(), constraint);
        } else {
            work();
            return true;
        }
    }

    private boolean add(CCheckRelation constraint) {
        if(!solve(constraint)) {
            return deferedChecks.add(constraint);
        } else {
            work();
            return true;
        }

    }

    private boolean add(CEvalFunction constraint) {
        tracker().addActive(constraint.getResult(), constraint);
        if(!solve(constraint)) {
            return deferedChecks.add(constraint);
        } else {
            work();
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    private boolean solve(CBuildRelation c) {
        ITerm left = find(c.getLeft());
        if(!left.isGround()) {
            return false;
        }
        ITerm right = find(c.getRight());
        if(!right.isGround()) {
            return false;
        }
        try {
            relations.add(c.getRelation(), left, right);
        } catch(RelationException e) {
            addMessage(c.getMessageInfo().withDefaultContent(MessageContent.of(e.getMessage())));
        }
        return true;
    }

    private boolean solve(CCheckRelation c) {
        if(!isComplete(c.getRelation())) {
            return false;
        }
        ITerm left = find(c.getLeft());
        ITerm right = find(c.getRight());
        if(!(left.isGround() && right.isGround())) {
            return false;
        }
        return relations.contains(c.getRelation(), left, right);
    }

    private boolean solve(CEvalFunction c) {
        ITerm term = find(c.getTerm());
        if(!term.isGround()) {
            return false;
        }
        PartialFunction1<ITerm, ITerm> fun = functions.get(c.getFunction());
        if(fun == null) {
            throw new FunctionUndefinedException("Function " + c.getFunction() + " undefined.");
        }
        Optional<ITerm> result = fun.apply(term);
        if(!result.isPresent()) {
            return false;
        }
        tracker().removeActive(c.getResult(), c);
        unify(c.getResult(), result.get(), c.getMessageInfo());
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

    // ------------------------------------------------------------------------------------------------------//

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class RelationResult {

        @Value.Parameter public abstract IRelations.Immutable<ITerm> relations();

        @Value.Parameter public abstract java.util.Set<IRelationConstraint> residualConstraints();

    }

}