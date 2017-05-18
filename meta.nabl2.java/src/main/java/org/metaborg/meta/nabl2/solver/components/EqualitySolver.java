package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.equality.CEqual;
import org.metaborg.meta.nabl2.constraints.equality.CInequal;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.UnificationResult;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.unification.Unifiers;
import org.metaborg.meta.nabl2.util.Optionals;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Sets;

public class EqualitySolver extends SolverComponent<IEqualityConstraint> {

    private final Unifier.Transient unifier;
    private final Set<IEqualityConstraint> defered;

    public EqualitySolver(Solver solver, Unifier.Transient unifier) {
        super(solver);
        this.unifier = unifier;
        this.defered = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(IEqualityConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add));
    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        return doIterateAndAdd(defered, this::solve);
    }

    @Override protected Set<? extends IEqualityConstraint> doFinish(IMessageInfo messageInfo) {
        return defered;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit add(CEqual constraint) throws UnsatisfiableException {
        defered.addAll(Optionals.ifThenElse(solve(constraint), cc -> {
            work();
            return cc;
        }, () -> Collections.singleton(constraint)));
        return unit;
    }

    private Unit add(CInequal constraint) throws UnsatisfiableException {
        defered.addAll(Optionals.ifThenElse(solve(constraint), cc -> {
            work();
            return cc;
        }, () -> Collections.singleton(constraint)));
        return unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<Set<IEqualityConstraint>> solve(IEqualityConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(IEqualityConstraint.CheckedCases.of(this::solve, this::solve));
    }

    private Optional<Set<IEqualityConstraint>> solve(CEqual constraint) throws UnsatisfiableException {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        try {
            final UnificationResult result = unifier.unify(left, right);
            if(result.getSubstituted().isEmpty() && !result.getResidual().isEmpty()) {
                return Optional.empty(); // no progress was made
            }
            tracker().updateActive(result.getSubstituted());
            return Optional.of(result.getResidual().stream()
                    .map(entry -> ImmutableCEqual.of(entry.getKey(), entry.getValue(), constraint.getMessageInfo()))
                    .collect(Collectors.toSet()));
        } catch(UnificationException ex) {
            MessageContent content = MessageContent.builder().append("Cannot unify ").append(left).append(" with ")
                    .append(right).build();
            throw new UnsatisfiableException(constraint.getMessageInfo().withDefaultContent(content));
        }
    }

    private Optional<Set<IEqualityConstraint>> solve(CInequal constraint) throws UnsatisfiableException {
        ITerm left = unifier.find(constraint.getLeft());
        ITerm right = unifier.find(constraint.getRight());
        if(left.equals(right)) {
            MessageContent content = MessageContent.builder().append(constraint.getLeft().toString()).append(" and ")
                    .append(constraint.getRight().toString()).append(" must be inequal, but both resolve to ")
                    .append(constraint.getLeft()).build();
            throw new UnsatisfiableException(constraint.getMessageInfo().withDefaultContent(content));
        }
        return Unifiers.canUnify(left, right) ? Optional.empty() : Optional.of(Collections.emptySet());
    }

}