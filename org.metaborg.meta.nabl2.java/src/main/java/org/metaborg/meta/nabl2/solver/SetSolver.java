package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.ImmutableMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.sets.CDistinct;
import org.metaborg.meta.nabl2.constraints.sets.CSubsetEq;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint.CheckedCases;
import org.metaborg.meta.nabl2.sets.IElement;
import org.metaborg.meta.nabl2.sets.SetEvaluator;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class SetSolver implements ISolverComponent<ISetConstraint> {

    private static final String NAME_OP = "NAME";

    private final IMatcher<Set<IElement<ITerm>>> evaluator;
    private final Unifier unifier;

    private final Set<ISetConstraint> defered;

    public SetSolver(IMatcher<Set<IElement<ITerm>>> elems, Unifier unifier) {
        this.evaluator = SetEvaluator.matcher(elems);
        this.unifier = unifier;
        this.defered = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(ISetConstraint constraint) throws UnsatisfiableException {
        if(!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        Iterator<ISetConstraint> it = defered.iterator();
        boolean progress = false;
        while(it.hasNext()) {
            try {
                if(solve(it.next())) {
                    progress = true;
                    it.remove();
                }
            } catch(UnsatisfiableException e) {
                progress = true;
                it.remove();
                throw e;
            }
        }
        return progress;
    }

    @Override public Iterable<IMessageInfo> finish() {
        return defered.stream().map(
            c -> c.getMessageInfo().withDefault(MessageContent.builder().append("Unsolved: ").append(c.pp()).build()))
            .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(ISetConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(CSubsetEq constraint) throws UnsatisfiableException {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        if(!left.isGround() && right.isGround()) {
            return false;
        }
        Optional<Set<IElement<ITerm>>> maybeLeftSet = evaluator.match(left);
        Optional<Set<IElement<ITerm>>> maybeRightSet = evaluator.match(right);
        if(!(maybeLeftSet.isPresent() && maybeRightSet.isPresent())) {
            return false;
        }
        Multimap<Object, IElement<ITerm>> leftProj =
            SetEvaluator.project(maybeLeftSet.get(), constraint.getProjection());
        Multimap<Object, IElement<ITerm>> rightProj =
            SetEvaluator.project(maybeRightSet.get(), constraint.getProjection());
        Multimap<Object, IElement<ITerm>> result = HashMultimap.create();
        result.putAll(leftProj);
        result.keySet().removeAll(rightProj.keySet());
        if(!result.isEmpty()) {
            MessageContent content = MessageContent.builder().append(GenericTerms.newAppl(NAME_OP)).append(" not in ")
                .append(constraint.getRight()).build();
            throw new UnsatisfiableException(
                makeMessages(constraint.getMessageInfo().withDefault(content), result.values()));
        }
        return true;
    }

    private boolean solve(CDistinct constraint) throws UnsatisfiableException {
        ITerm setTerm = unifier.find(constraint.getSet());
        if(!setTerm.isGround()) {
            return false;
        }
        Optional<Set<IElement<ITerm>>> maybeSet = evaluator.match(setTerm);
        if(!(maybeSet.isPresent())) {
            return false;
        }
        Multimap<Object, IElement<ITerm>> proj = SetEvaluator.project(maybeSet.get(), constraint.getProjection());
        List<IElement<ITerm>> duplicates = Lists.newArrayList();
        for(Object key : proj.keySet()) {
            Collection<IElement<ITerm>> values = proj.get(key);
            if(values.size() > 1) {
                duplicates.addAll(values);
            }
        }
        if(!duplicates.isEmpty()) {
            MessageContent content = MessageContent.builder().append(GenericTerms.newAppl(NAME_OP))
                .append(" has duplicates in ").append(constraint.getSet()).build();
            throw new UnsatisfiableException(
                makeMessages(constraint.getMessageInfo().withDefault(content), duplicates));
        }
        return true;
    }

    private Iterable<IMessageInfo> makeMessages(IMessageInfo template, Collection<IElement<ITerm>> elements) {
        boolean nameOrigin = M.appl0(NAME_OP).match(template.getOriginTerm()).isPresent();
        if(nameOrigin && !elements.isEmpty()) {
            return elements.stream().<IMessageInfo>map(e -> {
                Function1<ITerm, ITerm> f = M.sometd(M.appl0(NAME_OP, a -> e.getValue()));
                return ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f), e.getPosition());
            }).collect(Collectors.toList());
        } else {
            ITerm es = GenericTerms.newList(elements.stream().map(e -> e.getValue()).collect(Collectors.toList()));
            Function1<ITerm, ITerm> f = M.sometd(M.appl0(NAME_OP, a -> es));
            return Iterables2.singleton(
                ImmutableMessageInfo.of(template.getKind(), template.getContent().apply(f), template.getOriginTerm()));
        }
    }

    // ------------------------------------------------------------------------------------------------------//

}