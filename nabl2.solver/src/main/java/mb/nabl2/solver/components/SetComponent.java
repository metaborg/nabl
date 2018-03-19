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
import mb.nabl2.sets.IElement;
import mb.nabl2.sets.SetEvaluator;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.matching.Transform.T;

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
        ITerm left = unifier().findRecursive(constraint.getLeft());
        ITerm right = unifier().findRecursive(constraint.getRight());
        if(!left.isGround() && right.isGround()) {
            return Optional.empty();
        }
        Optional<Set<IElement<ITerm>>> maybeLeftSet = evaluator.match(left, unifier());
        Optional<Set<IElement<ITerm>>> maybeRightSet = evaluator.match(right, unifier());
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
                    MessageContent.builder().append(B.newAppl(NAME_OP)).append(" not in ").append(right).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), result.values());
            return Optional.of(SolveResult.messages(messages));
        }
    }

    private Optional<SolveResult> solve(CDistinct constraint) {
        ITerm setTerm = unifier().findRecursive(constraint.getSet());
        if(!setTerm.isGround()) {
            return Optional.empty();
        }
        Optional<Set<IElement<ITerm>>> maybeSet = evaluator.match(setTerm, unifier());
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
            MessageContent content = MessageContent.builder().append(B.newAppl(NAME_OP)).append(" has duplicates in ")
                    .append(setTerm).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), duplicates);
            return Optional.of(SolveResult.messages(messages));
        }
    }

    private Optional<SolveResult> solve(CEvalSet constraint) {
        ITerm setTerm = unifier().findRecursive(constraint.getSet());
        if(!setTerm.isGround()) {
            return Optional.empty();
        }
        Optional<Set<IElement<ITerm>>> maybeSet = evaluator.match(setTerm, unifier());
        if(!(maybeSet.isPresent())) {
            return Optional.empty();
        }
        List<ITerm> set =
                maybeSet.get().stream().map(i -> unifier().findRecursive(i.getValue())).collect(Collectors.toList());
        return Optional.of(SolveResult
                .constraints(ImmutableCEqual.of(constraint.getResult(), B.newList(set), constraint.getMessageInfo())));

    }

    private Iterable<IMessageInfo> makeMessages(IMessageInfo template, Collection<IElement<ITerm>> elements) {
        boolean nameOrigin = M.appl0(NAME_OP).match(template.getOriginTerm(), unifier()).isPresent();
        if(nameOrigin && !elements.isEmpty()) {
            return elements.stream().<IMessageInfo>map(e -> {
                Function1<ITerm, ITerm> f = T.sometd(t -> M.appl0(NAME_OP, a -> e.getValue()).match(t, unifier()));
                return ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f), e.getPosition());
            }).collect(Collectors.toList());
        } else {
            ITerm es = B.newList(elements.stream().map(e -> e.getValue()).collect(Collectors.toList()));
            Function1<ITerm, ITerm> f = T.sometd(t -> M.appl0(NAME_OP, a -> es).match(t, unifier()));
            return Iterables2.singleton(ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f),
                    template.getOriginTerm()));
        }
    }

}