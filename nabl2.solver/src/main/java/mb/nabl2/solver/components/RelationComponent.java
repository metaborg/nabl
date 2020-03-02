package mb.nabl2.solver.components;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.Iterables;
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
import mb.nabl2.solver.ISolver.SeedResult;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.FunctionUndefinedException;
import mb.nabl2.solver.exceptions.RelationDelayException;
import mb.nabl2.solver.exceptions.VariableDelayException;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;


public class RelationComponent extends ASolver {

    private final Predicate1<String> isComplete;

    private final Map<String, IVariantRelation.Transient<ITerm>> relations;
    private final Map<String, CheckedFunction1<ITerm, Optional<ITerm>, DelayException>> functions;

    public RelationComponent(SolverCore core, Predicate1<String> isComplete,
            Map<String, PartialFunction1<ITerm, ITerm>> functions,
            Map<String, IVariantRelation.Transient<ITerm>> relations) {
        super(core);
        this.isComplete = isComplete;
        this.relations = relations;
        this.functions = Maps.newHashMap();
        functions.forEach((name, f) -> this.functions.put(name, f::apply));
        addRelationFunctions();
    }

    private void addRelationFunctions() {
        for(String relationName : relations.keySet()) {
            String lubName = RelationFunctions.LUB.of(relationName);
            CheckedFunction1<ITerm, Optional<ITerm>, DelayException> lubFun = (term) -> {
                Optional<Tuple2<ITerm, ITerm>> pair =
                        M.tuple2(M.term(), M.term(), (t, l, r) -> (Tuple2<ITerm, ITerm>) ImmutableTuple2.of(l, r))
                                .match(term);
                if(pair.isPresent()) {
                    return lub(relationName, pair.get()._1(), pair.get()._2());
                } else {
                    return Optional.empty();
                }
            };
            functions.put(lubName, lubFun);
            String glbName = RelationFunctions.GLB.of(relationName);
            CheckedFunction1<ITerm, Optional<ITerm>, DelayException> glbFun = (term) -> {
                Optional<Tuple2<ITerm, ITerm>> pair =
                        M.tuple2(M.term(), M.term(), (t, l, r) -> (Tuple2<ITerm, ITerm>) ImmutableTuple2.of(l, r))
                                .match(term);
                if(pair.isPresent()) {
                    return glb(relationName, pair.get()._1(), pair.get()._2());
                } else {
                    return Optional.empty();
                }
            };
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

    public SolveResult solve(IRelationConstraint constraint) throws DelayException {
        return constraint.matchOrThrow(IRelationConstraint.CheckedCases.of(this::solve, this::solve, this::solve));
    }

    public Map<String, IVariantRelation.Immutable<ITerm>> finish() {
        return VariantRelations.freeze(relations);
    }

    // ------------------------------------------------------------------------------------------------------//

    public SolveResult solve(CBuildRelation c) throws DelayException {
        if(!(unifier().isGround(c.getLeft()) && unifier().isGround(c.getRight()))) {
            throw new VariableDelayException(
                    Iterables.concat(unifier().getVars(c.getLeft()), unifier().getVars(c.getRight())));
        }
        final ITerm left = unifier().findRecursive(c.getLeft());
        final ITerm right = unifier().findRecursive(c.getRight());
        // @formatter:off
        return c.getRelation().match(IRelationName.Cases.of(
            name -> {
                try {
                    relation(name).add(left, right);
                } catch(RelationException e) {
                    final IMessageInfo message = c.getMessageInfo().withDefaultContent(MessageContent.of(e.getMessage()));
                    return SolveResult.messages(message);
                }
                return SolveResult.empty();
            },
            extName -> {
                throw new IllegalArgumentException("Cannot add entries to external relations.");
            }
        ));
        // @formatter:on
    }

    public SolveResult solve(CCheckRelation c) throws DelayException {
        if(!(unifier().isGround(c.getLeft()) && unifier().isGround(c.getRight()))) {
            throw new VariableDelayException(
                    Iterables.concat(unifier().getVars(c.getLeft()), unifier().getVars(c.getRight())));
        }
        final ITerm left = unifier().findRecursive(c.getLeft());
        final ITerm right = unifier().findRecursive(c.getRight());
        // @formatter:off
        return c.getRelation().matchOrThrow(IRelationName.CheckedCases.of(
            name -> {
                if(!isComplete.test(name)) {
                    throw new RelationDelayException(name);
                }
                if(relation(name).contains(left, right)) {
                    return SolveResult.empty();
                } else {
                    IMessageInfo message = c.getMessageInfo().withDefaultContent(
                            MessageContent.builder().append(left).append(" and ").append(right).append(" not in ").append(name).build());
                    return SolveResult.messages(message);
                }
            },
            extName -> {
                final ITerm msginfo = MessageInfo.build(c.getMessageInfo());
                return callExternal(extName, left, right, msginfo).map(csTerm -> {
                    return Constraints.matchConstraintOrList().match(csTerm, unifier())
                            .map(SolveResult::constraints).orElseThrow(() -> new IllegalArgumentException("Expected list of constraints, got " + csTerm));
                }).orElse(SolveResult.messages(c.getMessageInfo()));
            }
        ));
        // @formatter:on
    }

    public SolveResult solve(CEvalFunction c) throws DelayException {
        if(!unifier().isGround(c.getTerm())) {
            throw new VariableDelayException(unifier().getVars(c.getTerm()));
        }
        final ITerm term = unifier().findRecursive(c.getTerm());
        // @formatter:off
        return c.getFunction().matchOrThrow(IFunctionName.CheckedCases.of(
            name -> {
                final CheckedFunction1<ITerm, Optional<ITerm>, DelayException> fun = functions.get(name);
                if(fun == null) {
                    throw new FunctionUndefinedException("Function " + name + " undefined.");
                }
                Optional<ITerm> result = fun.apply(term);
                IMessageInfo message = c.getMessageInfo().withDefaultContent(
                        MessageContent.builder().append(name).append(" failed on ").append(term).build());
                return result.map(ret -> {
                    return SolveResult.constraints(ImmutableCEqual.of(c.getResult(), ret, c.getMessageInfo()));
                }).orElse(SolveResult.messages(message));
            },
            extName -> {
                return callExternal(extName, term).map(ret -> {
                    return SolveResult.constraints(ImmutableCEqual.of(c.getResult(), ret, c.getMessageInfo()));
                }).orElse(SolveResult.messages(c.getMessageInfo()));
            }
        ));
        // @formatter:on
    }

    // ------------------------------------------------------------------------------------------------------//

    private IRelation.Transient<ITerm> relation(String name) {
        return Optional.ofNullable(relations.get(name))
                .orElseThrow(() -> new IllegalStateException("Relation <" + name + ": not defined."));
    }

    private Optional<ITerm> lub(String name, ITerm left, ITerm right) throws RelationDelayException {
        if(!(left.isGround() && right.isGround())) {
            throw new IllegalArgumentException("lub arguments need to be ground.");
        }
        if(!isComplete.test(name)) {
            throw new RelationDelayException(name);
        }
        return relation(name).leastUpperBound(left, right);
    }

    private Optional<ITerm> glb(String name, ITerm left, ITerm right) throws RelationDelayException {
        if(!isComplete.test(name)) {
            throw new RelationDelayException(name);
        }
        if(!(left.isGround() && right.isGround())) {
            throw new IllegalArgumentException("lub arguments need to be ground.");
        }
        return relation(name).greatestLowerBound(left, right);
    }

}