package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.equality.CEqual;
import org.metaborg.meta.nabl2.constraints.equality.CInequal;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutableSeedResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;
import org.metaborg.meta.nabl2.terms.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.UnificationMessages;

import com.google.common.collect.Sets;

public class EqualityComponent extends ASolver {

    private final IUnifier.Transient unifier;

    public EqualityComponent(SolverCore core, IUnifier.Transient initial) {
        super(core);
        this.unifier = initial;
    }

    public SeedResult seed(IUnifier.Immutable solution, IMessageInfo message) throws InterruptedException {
        final Set<IConstraint> constraints = Sets.newHashSet();
        final IMessages.Transient messages = Messages.Transient.of();
        for(ITermVar var : solution.varSet()) {
            try {
                unifier.unify(var, solution.findRecursive(var));
            } catch(UnificationException e) {
                messages.add(message.withContent(UnificationMessages.getError(e.getLeft(), e.getRight())));
            }
        }
        return ImmutableSeedResult.builder().constraints(constraints).messages(messages.freeze()).build();
    }

    public Optional<SolveResult> solve(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.of(this::solve, this::solve));
    }

    public IUnifier.Immutable finish() {
        return unifier.freeze();
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<SolveResult> solve(CEqual constraint) {
        ITerm left = unifier.findRecursive(constraint.getLeft());
        ITerm right = unifier.findRecursive(constraint.getRight());
        try {
            final IUnifier.Immutable unifyResult = unifier.unify(left, right);
            if(!unifyResult.isEmpty()) {
                final SolveResult solveResult = ImmutableSolveResult.builder().unifierDiff(unifyResult).build();
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
        ITerm left = unifier.findRecursive(constraint.getLeft());
        ITerm right = unifier.findRecursive(constraint.getRight());
        if(left.equals(right)) {
            MessageContent content = MessageContent.builder().append(constraint.getLeft().toString()).append(" and ")
                    .append(constraint.getRight().toString()).append(" must be inequal, but both resolve to ")
                    .append(constraint.getLeft()).build();
            IMessageInfo message = constraint.getMessageInfo().withDefaultContent(content);
            return Optional.of(SolveResult.messages(message));
        } else {
            return unifier.areUnequal(left, right) ? Optional.of(SolveResult.empty()) : Optional.empty();
        }
    }

}