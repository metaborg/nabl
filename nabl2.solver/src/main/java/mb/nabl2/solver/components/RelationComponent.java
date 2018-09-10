package mb.nabl2.solver.components;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.Maps;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.equality.ImmutableCEqual;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.constraints.relations.CBuildRelation;
import mb.nabl2.constraints.relations.CCheckRelation;
import mb.nabl2.constraints.relations.CEvalFunction;
import mb.nabl2.constraints.relations.IRelationConstraint;
import mb.nabl2.relations.IFunctionName;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.IRelationName;
import mb.nabl2.relations.RelationException;
import mb.nabl2.relations.terms.FunctionName.RelationFunctions;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.relations.variants.VariantRelations;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.FunctionUndefinedException;
import mb.nabl2.solver.ISolver.SeedResult;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;


public class RelationComponent extends ASolver {

    private final Predicate1<String> isComplete;

    private final Map<String, IVariantRelation.Transient<ITerm>> relations;
    private final Map<String, PartialFunction1<ITerm, ITerm>> functions;

    public RelationComponent(SolverCore core, Predicate1<String> isComplete,
            Map<String, PartialFunction1<ITerm, ITerm>> functions,
            Map<String, IVariantRelation.Transient<ITerm>> relations) {
        super(core);
        this.isComplete = isComplete;
        this.relations = relations;
        this.functions = Maps.newHashMap(functions);
        addRelationFunctions();
    }

    private void addRelationFunctions() {
        for(String relationName : relations.keySet()) {
            String lubName = RelationFunctions.LUB.of(relationName);
            PartialFunction1<ITerm, ITerm> lubFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return lub(relationName, left, right);
            }))::match;
            functions.put(lubName, lubFun);
            String glbName = RelationFunctions.GLB.of(relationName);
            PartialFunction1<ITerm, ITerm> glbFun = M.flatten(M.tuple2(M.term(), M.term(), (t, left, right) -> {
                return glb(relationName, left, right);
            }))::match;
            functions.put(glbName, glbFun);
        }
    }

    public SeedResult seed(Map<String, IVariantRelation.Immutable<ITerm>> solution,
            @SuppressWarnings("unused") IMessageInfo message) throws InterruptedException {
        for(Entry<String, IVariantRelation.Immutable<ITerm>> entry : solution.entrySet()) {
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

    public Map<String, IVariantRelation.Immutable<ITerm>> finish() {
        return VariantRelations.freeze(relations);
    }

    // ------------------------------------------------------------------------------------------------------//

    public Optional<SolveResult> solve(CBuildRelation c) {
        if(!(unifier().isGround(c.getLeft()) && unifier().isGround(c.getRight()))) {
            return Optional.empty();
        }
        final ITerm left = unifier().findRecursive(c.getLeft());
        final ITerm right = unifier().findRecursive(c.getRight());
        return c.getRelation().match(IRelationName.Cases.of(
        // @formatter:off
            name -> {
                try {
                    relation(name).add(left, right);
                } catch(RelationException e) {
                    final IMessageInfo message = c.getMessageInfo().withDefaultContent(MessageContent.of(e.getMessage()));
                    return Optional.of(SolveResult.messages(message));
                }
                return Optional.of(SolveResult.empty());
            },
            extName -> {
                throw new IllegalArgumentException("Cannot add entries to external relations.");
            }
            // @formatter:on
        ));
    }

    public Optional<SolveResult> solve(CCheckRelation c) {
        if(!(unifier().isGround(c.getLeft()) && unifier().isGround(c.getRight()))) {
            return Optional.empty();
        }
        final ITerm left = unifier().findRecursive(c.getLeft());
        final ITerm right = unifier().findRecursive(c.getRight());
        return c.getRelation().match(IRelationName.Cases.of(
        // @formatter:off
            name -> {
                if(!isComplete.test(name)) {
                    return Optional.empty();
                }
                if(relation(name).contains(left, right)) {
                    return Optional.of(SolveResult.empty());
                } else {
                    return Optional.empty();
                }
            },
            extName -> {
                final ITerm msginfo = MessageInfo.build(c.getMessageInfo());
                return callExternal(extName, left, right, msginfo).map(csTerm -> {
                    return Constraints.matchConstraintOrList().match(csTerm, unifier())
                            .map(SolveResult::constraints).orElseThrow(() -> new IllegalArgumentException("Expected list of constraints, got " + csTerm));
                });
            }
            // @formatter:on
        ));
    }

    public Optional<SolveResult> solve(CEvalFunction c) {
        if(!unifier().isGround(c.getTerm())) {
            return Optional.empty();
        }
        final ITerm term = unifier().findRecursive(c.getTerm());
        return c.getFunction().match(IFunctionName.Cases.of(
        // @formatter:off
            name -> {
                final PartialFunction1<ITerm, ITerm> fun = functions.get(name);
                if(fun == null) {
                    throw new FunctionUndefinedException("Function " + name + " undefined.");
                }
                Optional<ITerm> result = fun.apply(term);
                return result.map(ret -> {
                    return SolveResult.constraints(ImmutableCEqual.of(c.getResult(), ret, c.getMessageInfo()));
                });
            },
            extName -> {
                return callExternal(extName, term).map(ret -> {
                    return SolveResult.constraints(ImmutableCEqual.of(c.getResult(), ret, c.getMessageInfo()));
                });
            }
            // @formatter:on
        ));
    }

    // ------------------------------------------------------------------------------------------------------//

    private IRelation.Transient<ITerm> relation(String name) {
        return Optional.ofNullable(relations.get(name))
                .orElseThrow(() -> new IllegalStateException("Relation <" + name + ": not defined."));
    }

    private Optional<ITerm> lub(String name, ITerm left, ITerm right) {
        if(!(left.isGround() && right.isGround())) {
            throw new IllegalArgumentException("lub arguments need to be ground.");
        }
        if(!isComplete.test(name)) {
            return Optional.empty();
        }
        return relation(name).leastUpperBound(left, right);
    }

    private Optional<ITerm> glb(String name, ITerm left, ITerm right) {
        if(!isComplete.test(name)) {
            return Optional.empty();
        }
        if(!(left.isGround() && right.isGround())) {
            throw new IllegalArgumentException("lub arguments need to be ground.");
        }
        return relation(name).greatestLowerBound(left, right);
    }

}