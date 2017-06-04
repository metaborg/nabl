package org.metaborg.meta.nabl2.solver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.util.time.AggregateTimer;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public interface ISolver<C extends IConstraint, R> {

    SeedResult seed(R solution, IMessageInfo message) throws InterruptedException;

    Optional<SolveResult> solve(C constraint) throws InterruptedException;

    boolean update() throws InterruptedException;

    R finish();

    AggregateTimer getTimer();

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

        @Value.Default public Multimap<String, String> strongDependencies() {
            return ImmutableMultimap.of();
        }

        @Value.Default public Multimap<String, String> weakDependencies() {
            return ImmutableMultimap.of();
        }

        public static SolveResult empty() {
            return ImmutableSolveResult.builder().build();
        }

        public static SolveResult messages(IMessageInfo... messages) {
            return messages(Arrays.asList(messages));
        }

        public static SolveResult messages(Iterable<? extends IMessageInfo> messages) {
            Messages.Transient msgs = Messages.Transient.of();
            msgs.addAll(messages);
            return ImmutableSolveResult.builder().messages(msgs.freeze()).build();
        }

        public static SolveResult constraints(IConstraint... constraints) {
            return constraints(Arrays.asList(constraints));
        }

        public static SolveResult constraints(Iterable<? extends IConstraint> constraints) {
            return ImmutableSolveResult.builder().constraints(constraints).build();
        }

    }

    public static <C extends IConstraint, R> ISolver<C, R> defer(final ISolver<C, R> component) {
        return new ISolver<C, R>() {

            public SeedResult seed(R solution, IMessageInfo message) throws InterruptedException {
                return component.seed(solution, message);
            }

            public Optional<SolveResult> solve(C constraint) throws InterruptedException {
                return Optional.empty();
            }

            public boolean update() throws InterruptedException {
                return false;
            }

            public R finish() {
                return component.finish();
            }

            public AggregateTimer getTimer() {
                return component.getTimer();
            }

        };
    }

    public static <C extends IConstraint, R> ISolver<C, R> deny(final ISolver<C, R> component) {
        return new ISolver<C, R>() {

            public SeedResult seed(R solution, IMessageInfo message) throws InterruptedException {
                return component.seed(solution, message);
            }

            public Optional<SolveResult> solve(C constraint) throws InterruptedException {
                throw new IllegalStateException("Solving is not allowed for " + constraint);
            }

            public boolean update() throws InterruptedException {
                return false;
            }

            public R finish() {
                return component.finish();
            }

            public AggregateTimer getTimer() {
                return component.getTimer();
            }

        };
    }

    public static <C extends IConstraint, R> ISolver<C, R> ignore(final ISolver<C, R> component) {
        return new ISolver<C, R>() {

            public SeedResult seed(R solution, IMessageInfo message) throws InterruptedException {
                return component.seed(solution, message);
            }

            public Optional<SolveResult> solve(C constraint) throws InterruptedException {
                return Optional.of(SolveResult.empty());
            }

            public boolean update() throws InterruptedException {
                return false;
            }

            public R finish() {
                return component.finish();
            }

            public AggregateTimer getTimer() {
                return component.getTimer();
            }

        };
    }

}