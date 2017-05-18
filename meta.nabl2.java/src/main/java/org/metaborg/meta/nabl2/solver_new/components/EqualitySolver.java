package org.metaborg.meta.nabl2.solver_new.components;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.equality.CEqual;
import org.metaborg.meta.nabl2.constraints.equality.CInequal;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.unification.UnificationResult;
import org.metaborg.meta.nabl2.unification.Unifiers;
import org.metaborg.meta.nabl2.util.Optionals;

import com.google.common.collect.Sets;

public class EqualitySolver extends ASolver<IEqualityConstraint, EqualitySolver.EqualityResult> {

    private final java.util.Set<IEqualityConstraint> constraints;

    public EqualitySolver(SolverCore core, EqualityResult... results) {
        super(core);
        this.constraints = Sets.newHashSet();
        for(EqualityResult result : results) {
            constraints.addAll(result.residualConstraints());
        }
    }

    @Override public boolean add(IEqualityConstraint constraint) throws InterruptedException {
        return constraint.match(IEqualityConstraint.Cases.of(this::add, this::add));
    }

    @Override public boolean iterate() throws InterruptedException {
        return doIterateAndAdd(constraints, this::solve);
    }

    public EqualityResult finish() {
        return ImmutableEqualityResult.of(constraints);
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean add(CEqual constraint) {
        constraints.addAll(Optionals.ifThenElse(solve(constraint), cc -> {
            work();
            return cc;
        }, () -> Collections.singleton(constraint)));
        return true;
    }

    private boolean add(CInequal constraint) {
        constraints.addAll(Optionals.ifThenElse(solve(constraint), cc -> {
            work();
            return cc;
        }, () -> Collections.singleton(constraint)));
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Optional<java.util.Set<IEqualityConstraint>> solve(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.of(this::solve, this::solve));
    }

    private Optional<java.util.Set<IEqualityConstraint>> solve(CEqual constraint) {
        ITerm left = find(constraint.getLeft());
        ITerm right = find(constraint.getRight());
        try {
            final UnificationResult result = unify(left, right);
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
            addMessage(constraint.getMessageInfo().withDefaultContent(content));
            return Optional.of(Collections.emptySet());
        }
    }

    private Optional<java.util.Set<IEqualityConstraint>> solve(CInequal constraint) {
        ITerm left = find(constraint.getLeft());
        ITerm right = find(constraint.getRight());
        if(left.equals(right)) {
            MessageContent content = MessageContent.builder().append(constraint.getLeft().toString()).append(" and ")
                    .append(constraint.getRight().toString()).append(" must be inequal, but both resolve to ")
                    .append(constraint.getLeft()).build();
            addMessage(constraint.getMessageInfo().withDefaultContent(content));
            return Optional.of(Collections.emptySet());
        } else {
            return Unifiers.canUnify(left, right) ? Optional.empty() : Optional.of(Collections.emptySet());
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class EqualityResult {

        @Value.Parameter public abstract java.util.Set<IEqualityConstraint> residualConstraints();

    }

}