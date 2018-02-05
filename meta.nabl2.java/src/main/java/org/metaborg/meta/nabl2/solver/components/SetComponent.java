package org.metaborg.meta.nabl2.solver.components;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.ImmutableMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.sets.CDistinct;
import org.metaborg.meta.nabl2.constraints.sets.CEvalSet;
import org.metaborg.meta.nabl2.constraints.sets.CSubsetEq;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.sets.IElement;
import org.metaborg.meta.nabl2.sets.SetEvaluator;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class SetComponent extends ASolver {

    private static final String NAME_OP = "NAME";

    private final IMatcher<Set<IElement<ITerm>>> evaluator;

    public SetComponent(SolverCore core, IMatcher<Set<IElement<ITerm>>> elems) {
        super(core);
        this.evaluator = SetEvaluator.matcher(elems);
    }

    public Optional<SolveResult> solve(ISetConstraint constraint) {
        return constraint.match(ISetConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    public Unit finish() {
        return Unit.unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CSubsetEq constraint) {
        ITerm left = find(constraint.getLeft());
        ITerm right = find(constraint.getRight());
        if(!left.isGround() && right.isGround()) {
            return Optional.empty();
        }
        Optional<Set<IElement<ITerm>>> maybeLeftSet = evaluator.match(left);
        Optional<Set<IElement<ITerm>>> maybeRightSet = evaluator.match(right);
        if(!(maybeLeftSet.isPresent() && maybeRightSet.isPresent())) {
            return Optional.empty();
        }
        Multimap<Object, IElement<ITerm>> leftProj =
                SetEvaluator.project(maybeLeftSet.get(), constraint.getProjection());
        Multimap<Object, IElement<ITerm>> rightProj =
                SetEvaluator.project(maybeRightSet.get(), constraint.getProjection());
        Multimap<Object, IElement<ITerm>> result = HashMultimap.create();
        result.putAll(leftProj);
        result.keySet().removeAll(rightProj.keySet());
        if(result.isEmpty()) {
            return Optional.of(SolveResult.empty());
        } else {
            MessageContent content =
                    MessageContent.builder().append(TB.newAppl(NAME_OP)).append(" not in ").append(right).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), result.values());
            return Optional.of(SolveResult.messages(messages));
        }
    }

    private Optional<SolveResult> solve(CDistinct constraint) {
        ITerm setTerm = find(constraint.getSet());
        if(!setTerm.isGround()) {
            return Optional.empty();
        }
        Optional<Set<IElement<ITerm>>> maybeSet = evaluator.match(setTerm);
        if(!(maybeSet.isPresent())) {
            return Optional.empty();
        }
        Multimap<Object, IElement<ITerm>> proj = SetEvaluator.project(maybeSet.get(), constraint.getProjection());
        List<IElement<ITerm>> duplicates = Lists.newArrayList();
        for(Object key : proj.keySet()) {
            Collection<IElement<ITerm>> values = proj.get(key);
            if(values.size() > 1) {
                duplicates.addAll(values);
            }
        }
        if(duplicates.isEmpty()) {
            return Optional.of(SolveResult.empty());
        } else {
            MessageContent content = MessageContent.builder().append(TB.newAppl(NAME_OP)).append(" has duplicates in ")
                    .append(setTerm).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), duplicates);
            return Optional.of(SolveResult.messages(messages));
        }
    }

    private Optional<SolveResult> solve(CEvalSet constraint) {
        ITerm setTerm = find(constraint.getSet());
        if(!setTerm.isGround()) {
            return Optional.empty();
        }
        Optional<Set<IElement<ITerm>>> maybeSet = evaluator.match(setTerm);
        if(!(maybeSet.isPresent())) {
            return Optional.empty();
        }
        List<ITerm> set = maybeSet.get().stream().map(i -> find(i.getValue())).collect(Collectors.toList());
        return Optional.of(SolveResult
                .constraints(ImmutableCEqual.of(constraint.getResult(), TB.newList(set), constraint.getMessageInfo())));

    }

    private Iterable<IMessageInfo> makeMessages(IMessageInfo template, Collection<IElement<ITerm>> elements) {
        boolean nameOrigin = M.appl0(NAME_OP).match(template.getOriginTerm()).isPresent();
        if(nameOrigin && !elements.isEmpty()) {
            return elements.stream().<IMessageInfo>map(e -> {
                Function1<ITerm, ITerm> f = M.sometd(M.appl0(NAME_OP, a -> e.getValue()));
                return ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f), e.getPosition());
            }).collect(Collectors.toList());
        } else {
            ITerm es = TB.newList(elements.stream().map(e -> e.getValue()).collect(Collectors.toList()));
            Function1<ITerm, ITerm> f = M.sometd(M.appl0(NAME_OP, a -> es));
            return Iterables2.singleton(ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f),
                    template.getOriginTerm()));
        }
    }

}