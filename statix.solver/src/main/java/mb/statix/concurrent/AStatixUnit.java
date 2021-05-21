package mb.statix.concurrent;

import java.util.Optional;

import org.immutables.value.Value;

import mb.statix.spec.Rule;

@Value.Immutable
public abstract class AStatixUnit implements IStatixUnit {

    @Value.Parameter @Override public abstract String resource();

    @Value.Parameter @Override public abstract Optional<Rule> rule();

    @Value.Parameter @Override public abstract boolean changed();

    @Override public String toString() {
        return "StatixUnit@" + System.identityHashCode(this);
    }

}