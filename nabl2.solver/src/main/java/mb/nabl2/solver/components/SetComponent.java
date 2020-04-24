package mb.nabl2.solver.components;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import mb.nabl2.constraints.equality.ImmutableCEqual;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.ImmutableMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.sets.CDistinct;
import mb.nabl2.constraints.sets.CEvalSet;
import mb.nabl2.constraints.sets.CSubsetEq;
import mb.nabl2.constraints.sets.ISetConstraint;
import mb.nabl2.scopegraph.esop.CriticalEdgeException;
import mb.nabl2.sets.IElement;
import mb.nabl2.sets.ISetProducer;
import mb.nabl2.sets.SetEvaluator;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.exceptions.CriticalEdgeDelayException;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.VariableDelayException;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.matching.Transform.T;

public class SetComponent extends ASolver {

    private static final String NAME_OP = "NAME";

    private final IMatcher<ISetProducer<ITerm>> evaluator;

    public SetComponent(SolverCore core, IMatcher<ISetProducer<ITerm>> elems) {
        super(core);
        this.evaluator = SetEvaluator.matcher(elems);
    }

    public SolveResult solve(ISetConstraint constraint) throws DelayException {
        return constraint.matchOrThrow(ISetConstraint.CheckedCases.of(this::solve, this::solve, this::solve));
    }

    public Unit finish() {
        return Unit.unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private SolveResult solve(CSubsetEq constraint) throws DelayException {
        ITerm left = constraint.getLeft();
        ITerm right = constraint.getRight();
        if(!unifier().isGround(left) && unifier().isGround(right)) {
            throw new VariableDelayException(Iterables.concat(unifier().getVars(left), unifier().getVars(right)));
        }
        Optional<ISetProducer<ITerm>> maybeLeftSet = evaluator.match(left, unifier());
        Optional<ISetProducer<ITerm>> maybeRightSet = evaluator.match(right, unifier());
        if(!(maybeLeftSet.isPresent() && maybeRightSet.isPresent())) {
            return SolveResult.empty();
        }
        final Set<IElement<ITerm>> leftSet;
        final Set<IElement<ITerm>> rightSet;
        try {
            leftSet = maybeLeftSet.get().apply();
            rightSet = maybeRightSet.get().apply();
        } catch(CriticalEdgeException e) {
            throw new CriticalEdgeDelayException(e);
        }
        Multimap<Object, IElement<ITerm>> leftProj = SetEvaluator.project(leftSet, constraint.getProjection());
        Multimap<Object, IElement<ITerm>> rightProj = SetEvaluator.project(rightSet, constraint.getProjection());
        Multimap<Object, IElement<ITerm>> result = HashMultimap.create();
        result.putAll(leftProj);
        result.keySet().removeAll(rightProj.keySet());
        if(result.isEmpty()) {
            return SolveResult.empty();
        } else {
            MessageContent content =
                    MessageContent.builder().append(B.newAppl(NAME_OP)).append(" not in ").append(right).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), result.values());
            return SolveResult.messages(messages);
        }
    }

    private SolveResult solve(CDistinct constraint) throws DelayException {
        ITerm setTerm = constraint.getSet();
        if(!unifier().isGround(setTerm)) {
            throw new VariableDelayException(unifier().getVars(setTerm));
        }
        Optional<ISetProducer<ITerm>> maybeSet = evaluator.match(setTerm, unifier());
        if(!(maybeSet.isPresent())) {
            return SolveResult.empty();
        }
        Set<IElement<ITerm>> set;
        try {
            set = maybeSet.get().apply();
        } catch(CriticalEdgeException e) {
            throw new CriticalEdgeDelayException(e);
        }
        Multimap<Object, IElement<ITerm>> proj = SetEvaluator.project(set, constraint.getProjection());
        List<IElement<ITerm>> duplicates = Lists.newArrayList();
        for(Object key : proj.keySet()) {
            Collection<IElement<ITerm>> values = proj.get(key);
            if(values.size() > 1) {
                duplicates.addAll(values);
            }
        }
        if(duplicates.isEmpty()) {
            return SolveResult.empty();
        } else {
            MessageContent content = MessageContent.builder().append(B.newAppl(NAME_OP)).append(" has duplicates in ")
                    .append(setTerm).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), duplicates);
            return SolveResult.messages(messages);
        }
    }

    private SolveResult solve(CEvalSet constraint) throws DelayException {
        ITerm setTerm = constraint.getSet();
        if(!unifier().isGround(setTerm)) {
            throw new VariableDelayException(unifier().getVars(setTerm));
        }
        Optional<ISetProducer<ITerm>> maybeSet = evaluator.match(setTerm, unifier());
        if(!(maybeSet.isPresent())) {
            return SolveResult.empty();
        }
        Set<IElement<ITerm>> set;
        try {
            set = maybeSet.get().apply();
        } catch(CriticalEdgeException e) {
            throw new CriticalEdgeDelayException(e);
        }
        List<ITerm> elements = set.stream().map(i -> i.getValue()).collect(Collectors.toList());
        return SolveResult.constraints(
                ImmutableCEqual.of(constraint.getResult(), B.newList(elements), constraint.getMessageInfo()));

    }

    private Iterable<IMessageInfo> makeMessages(IMessageInfo template, Collection<IElement<ITerm>> elements) {
        boolean nameOrigin = M.appl0(NAME_OP).match(template.getOriginTerm(), unifier()).isPresent();
        if(nameOrigin && !elements.isEmpty()) {
            return elements.stream().<IMessageInfo>map(e -> {
                Function1<ITerm, ITerm> f = T.sometd(t -> M.appl0(NAME_OP, a -> e.getName()).match(t, unifier()));
                return ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f), e.getPosition());
            }).collect(Collectors.toList());
        } else {
            ITerm es = B.newList(elements.stream().map(e -> e.getName()).collect(Collectors.toList()));
            Function1<ITerm, ITerm> f = T.sometd(t -> M.appl0(NAME_OP, a -> es).match(t, unifier()));
            return Iterables2.singleton(ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f),
                    template.getOriginTerm()));
        }
    }

}