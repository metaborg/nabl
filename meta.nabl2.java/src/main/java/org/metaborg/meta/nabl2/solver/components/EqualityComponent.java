package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.equality.CEqual;
import org.metaborg.meta.nabl2.constraints.equality.CInequal;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ImmutableSeedResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.UnificationResult;
import org.metaborg.meta.nabl2.unification.Unifiers;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.collect.Sets;

public class EqualityComponent extends ASolver<IEqualityConstraint, IUnifier.Immutable> {

    private final IUnifier.Transient unifier;

    public EqualityComponent(SolverCore core, IUnifier.Transient initial) {
        super(core);
        this.unifier = initial;
    }

    @Override public SeedResult seed(IUnifier.Immutable solution, IMessageInfo message) throws InterruptedException {
        final Set<IConstraint> constraints = Sets.newHashSet();
        final IMessages.Transient messages = Messages.Transient.of();
        for(Tuple2<ITermVar, ITerm> vt : (Iterable<Tuple2<ITermVar, ITerm>>) solution.stream()::iterator) {
            try {
                final UnificationResult result = unifier.unify(vt._1(), vt._2());
                result.getResidual().stream().map(r -> ImmutableCEqual.of(r.getKey(), r.getValue(), message))
                        .forEach(constraints::add);
            } catch(UnificationException e) {
                messages.add(message.withContent(e.getMessageContent()));
            }
        }
        return ImmutableSeedResult.builder().constraints(constraints).messages(messages.freeze()).build();
    }

    @Override public Optional<SolveResult> solve(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.of(this::solve, this::solve));
    }

    public IUnifier.Immutable finish() {
        return unifier.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CEqual constraint) {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        try {
            final UnificationResult unifyResult = unifier.unify(left, right);
            if(!unifyResult.getSubstituted().isEmpty() || unifyResult.getResidual().isEmpty()) {
                final Set<IConstraint> constraints = unifyResult.getResidual().stream()
                        .map(ts -> ImmutableCEqual.of(ts.getKey(), ts.getValue(), constraint.getMessageInfo()))
                        .collect(Collectors.toSet());
                final SolveResult solveResult = ImmutableSolveResult.builder().constraints(constraints).build();
                return Optional.of(solveResult);
            } else {
                return Optional.empty();
            }
        } catch(UnificationException ex) {
            final MessageContent content = MessageContent.builder().append("Cannot unify ").append(left)
                    .append(" with ").append(right).build();
            final IMessageInfo message = (constraint.getMessageInfo().withDefaultContent(content));
            return Optional.of(SolveResult.messages(message));
        }
    }

    private Optional<SolveResult> solve(CInequal constraint) {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        if(left.equals(right)) {
            MessageContent content = MessageContent.builder().append(constraint.getLeft().toString()).append(" and ")
                    .append(constraint.getRight().toString()).append(" must be inequal, but both resolve to ")
                    .append(constraint.getLeft()).build();
            IMessageInfo message = constraint.getMessageInfo().withDefaultContent(content);
            return Optional.of(SolveResult.messages(message));
        } else {
            return Unifiers.canUnify(left, right) ? Optional.empty() : Optional.of(SolveResult.empty());
        }
    }

}