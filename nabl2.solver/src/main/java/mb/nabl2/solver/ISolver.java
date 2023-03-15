package mb.nabl2.solver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.CheckedFunction1;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.UnconditionalDelayExpection;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;

@FunctionalInterface
public interface ISolver extends CheckedFunction1<IConstraint, SolveResult, DelayException> {

    default void update(@SuppressWarnings("unused") Collection<ITermVar> vars) {
        // ignore by default
    };

    @Value.Immutable(builder = true)
    @Serial.Version(42l)
    public static abstract class ASeedResult {

        @Value.Default public Set.Immutable<IConstraint> constraints() {
            return CapsuleUtil.immutableSet();
        }

        @Value.Default public IMessages.Immutable messages() {
            return Messages.Immutable.of();
        }

        public static SeedResult empty() {
            return SeedResult.builder().build();
        }

        public static SeedResult messages(IMessageInfo... messages) {
            return messages(Arrays.asList(messages));
        }

        public static SeedResult messages(Iterable<? extends IMessageInfo> messages) {
            Messages.Transient msgs = Messages.Transient.of();
            msgs.addAll(messages);
            return SeedResult.builder().messages(msgs.freeze()).build();
        }

        public static SeedResult constraints(IConstraint... constraints) {
            return constraints(Arrays.asList(constraints));
        }

        public static SeedResult constraints(Iterable<? extends IConstraint> constraints) {
            return SeedResult.builder().constraints(constraints).build();
        }

    }

    @Value.Immutable(builder = true)
    @Serial.Version(42l)
    public static abstract class ASolveResult {

        @Value.Default public Set.Immutable<IConstraint> constraints() {
            return CapsuleUtil.immutableSet();
        }

        @Value.Default public IMessages.Immutable messages() {
            return Messages.Immutable.of();
        }

        @Value.Default public IUnifier.Immutable unifierDiff() {
            return Unifiers.Immutable.of();
        }

        public static SolveResult empty() {
            return SolveResult.builder().build();
        }

        public static SolveResult messages(IMessageInfo... messages) {
            return messages(Arrays.asList(messages));
        }

        public static SolveResult messages(Iterable<? extends IMessageInfo> messages) {
            Messages.Transient msgs = Messages.Transient.of();
            msgs.addAll(messages);
            return SolveResult.builder().messages(msgs.freeze()).build();
        }

        public static SolveResult constraints(IConstraint... constraints) {
            return constraints(Arrays.asList(constraints));
        }

        public static SolveResult constraints(Collection<? extends IConstraint> constraints) {
            return SolveResult.builder().constraints(CapsuleUtil.toSet(constraints)).build();
        }

    }

    public static ISolver deny(String error) {
        return c -> {
            throw new IllegalArgumentException(error + ": " + c);
        };
    }

    public static ISolver defer() {
        return c -> {
            throw new UnconditionalDelayExpection();
        };
    }

    public static ISolver drop() {
        return c -> SolveResult.empty();
    }

}