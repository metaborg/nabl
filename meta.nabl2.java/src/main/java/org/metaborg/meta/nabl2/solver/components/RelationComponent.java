package org.metaborg.meta.nabl2.solver.components;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.relations.CBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.CCheckRelation;
import org.metaborg.meta.nabl2.constraints.relations.CEvalFunction;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms.RelationFunctions;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.relations.variants.VariantRelations;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.FunctionUndefinedException;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.Maps;


public class RelationComponent extends ASolver {

    private final Predicate1<IRelationName> isComplete;

    private final Map<IRelationName, IVariantRelation.Transient<ITerm>> relations;
    private final Map<String, PartialFunction1<ITerm, ITerm>> functions;

    public RelationComponent(SolverCore core, Predicate1<IRelationName> isComplete,
            Map<String, PartialFunction1<ITerm, ITerm>> functions,
            Map<IRelationName, IVariantRelation.Transient<ITerm>> initial) {
        super(core);
        this.isComplete = isComplete;
        this.relations = initial;
        this.functions = Maps.newHashMap(functions);
        addRelationFunctions();
    }

    private void addRelationFunctions() {
        for(IRelationName relationName : relations.keySet()) {
            String lubName = RelationTerms.relationFunction(relationName, RelationFunctions.LUB);
            PartialFunction1<ITerm, ITerm> lubFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return lub(relationName, left, right);
            }))::match;
            functions.put(lubName, lubFun);
            String glbName = RelationTerms.relationFunction(relationName, RelationFunctions.GLB);
            PartialFunction1<ITerm, ITerm> glbFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return glb(relationName, left, right);
            }))::match;
            functions.put(glbName, glbFun);
        }
    }

    public SeedResult seed(Map<IRelationName, IVariantRelation.Immutable<ITerm>> solution, IMessageInfo message)
            throws InterruptedException {
        for(Entry<IRelationName, IVariantRelation.Immutable<ITerm>> entry : solution.entrySet()) {
            try {
                relation(entry.getKey()).addAll(entry.getValue());
            } catch(RelationException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return SeedResult.empty();
    }

    public Optional<SolveResult> solve(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    public Map<IRelationName, IVariantRelation.Immutable<ITerm>> finish() {
        return VariantRelations.freeze(relations);
    }

    // ------------------------------------------------------------------------------------------------------//

    public Optional<SolveResult> solve(CBuildRelation c) {
        final ITerm left = find(c.getLeft());
        final ITerm right = find(c.getRight());
        if(!(left.isGround() && right.isGround())) {
            return Optional.empty();
        }
        try {
            relation(c.getRelation()).add(left, right);
        } catch(RelationException e) {
            final IMessageInfo message = c.getMessageInfo().withDefaultContent(MessageContent.of(e.getMessage()));
            return Optional.of(SolveResult.messages(message));
        }
        return Optional.of(SolveResult.empty());
    }

    public Optional<SolveResult> solve(CCheckRelation c) {
        if(!isComplete.test(c.getRelation())) {
            return Optional.empty();
        }
        final ITerm left = find(c.getLeft());
        final ITerm right = find(c.getRight());
        if(!(left.isGround() && right.isGround())) {
            return Optional.empty();
        }
        if(relation(c.getRelation()).contains(left, right)) {
            return Optional.of(SolveResult.empty());
        } else {
            return Optional.empty();
        }
    }

    public Optional<SolveResult> solve(CEvalFunction c) {
        final ITerm term = find(c.getTerm());
        if(!term.isGround()) {
            return Optional.empty();
        }
        final PartialFunction1<ITerm, ITerm> fun = functions.get(c.getFunction());
        if(fun == null) {
            throw new FunctionUndefinedException("Function " + c.getFunction() + " undefined.");
        }
        Optional<ITerm> result = fun.apply(term);
        return result.map(ret -> {
            return SolveResult.constraints(ImmutableCEqual.of(c.getResult(), ret, c.getMessageInfo()));
        });
    }

    // ------------------------------------------------------------------------------------------------------//

    private IRelation.Transient<ITerm> relation(IRelationName name) {
        return Optional.ofNullable(relations.get(name))
                .orElseThrow(() -> new IllegalStateException("Relation " + name + " not defined."));
    }

    private Optional<ITerm> lub(IRelationName name, ITerm left, ITerm right) {
        if(!isComplete.test(name)) {
            return Optional.empty();
        }
        return relation(name).leastUpperBound(left, right);
    }

    private Optional<ITerm> glb(IRelationName name, ITerm left, ITerm right) {
        if(!isComplete.test(name)) {
            return Optional.empty();
        }
        return relation(name).greatestLowerBound(left, right);
    }

}