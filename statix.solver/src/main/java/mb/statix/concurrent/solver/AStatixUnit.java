package mb.statix.concurrent.solver;

import java.util.Optional;

import org.immutables.value.Value;

import mb.statix.spec.Rule;

@Value.Immutable
public abstract class AStatixUnit implements IStatixUnit {

    @Value.Parameter @Override public abstract String resource();

    @Value.Parameter @Override public abstract Optional<Rule> rule();

    @Override public String toString() {
        return "StatixUnit@" + System.identityHashCode(this);
    }

}