package org.metaborg.meta.nabl2.solver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.util.functions.CheckedFunction1;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

@FunctionalInterface
public interface ISolver extends CheckedFunction1<IConstraint, Optional<SolveResult>, InterruptedException> {

    @Value.Immutable(builder = true)
    @Serial.Version(42l)
    public static abstract class SeedResult {

        @Value.Default public Set<IConstraint> constraints() {
            return Collections.emptySet();
        }

        @Value.Default public IMessages.Immutable messages() {
            return Messages.Immutable.of();
        }

        public static SeedResult empty() {
            return ImmutableSeedResult.builder().build();
        }

        public static SeedResult messages(IMessageInfo... messages) {
            return messages(Arrays.asList(messages));
        }

        public static SeedResult messages(Iterable<? extends IMessageInfo> messages) {
            Messages.Transient msgs = Messages.Transient.of();
            msgs.addAll(messages);
            return ImmutableSeedResult.builder().messages(msgs.freeze()).build();
        }

        public static SeedResult constraints(IConstraint... constraints) {
            return constraints(Arrays.asList(constraints));
        }

        public static SeedResult constraints(Iterable<? extends IConstraint> constraints) {
            return ImmutableSeedResult.builder().constraints(constraints).build();
        }

    }

    @Value.Immutable(builder = true)
    @Serial.Version(42l)
    public static abstract class SolveResult {

        @Value.Default public Set<IConstraint> constraints() {
            return Collections.emptySet();
        }

        @Value.Default public IMessages.Immutable messages() {
            return Messages.Immutable.of();
        }

        @Value.Default public SetMultimap<String, String> dependencies() {
            return ImmutableSetMultimap.of();
        }

        @Value.Default public Set<ITermVar> unifiedVars() {
            return Collections.emptySet();
        }

        public static ImmutableSolveResult empty() {
            return ImmutableSolveResult.builder().build();
        }

        public static ImmutableSolveResult messages(IMessageInfo... messages) {
            return messages(Arrays.asList(messages));
        }

        public static ImmutableSolveResult messages(Iterable<? extends IMessageInfo> messages) {
            Messages.Transient msgs = Messages.Transient.of();
            msgs.addAll(messages);
            return ImmutableSolveResult.builder().messages(msgs.freeze()).build();
        }

        public static ImmutableSolveResult constraints(IConstraint... constraints) {
            return constraints(Arrays.asList(constraints));
        }

        public static ImmutableSolveResult constraints(Iterable<? extends IConstraint> constraints) {
            return ImmutableSolveResult.builder().constraints(constraints).build();
        }

    }

    public static ISolver deny(String error) {
        return c -> {
            throw new IllegalArgumentException(error + ": " + c);
        };
    }

    public static ISolver defer() {
        return c -> Optional.empty();
    }

    public static ISolver drop() {
        return c -> Optional.of(SolveResult.empty());
    }

}