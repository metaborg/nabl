package mb.nabl2.solver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.functions.CheckedFunction1;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;

@FunctionalInterface
public interface ISolver extends CheckedFunction1<IConstraint, Optional<SolveResult>, InterruptedException> {

    default void update(@SuppressWarnings("unused") Collection<ITermVar> vars) {
        // ignore by default
    };

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

        @Value.Default public IUnifier.Immutable unifierDiff() {
            return Unifiers.Immutable.of();
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