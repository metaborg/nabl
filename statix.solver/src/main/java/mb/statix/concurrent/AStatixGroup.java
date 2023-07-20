package mb.statix.concurrent;

import java.util.Optional;

import org.immutables.value.Value;
import org.metaborg.util.collection.ImList;

import io.usethesource.capsule.Map;
import mb.statix.spec.Rule;

@Value.Immutable
public abstract class AStatixGroup implements IStatixGroup {

    @Value.Parameter @Override public abstract String resource();

    @Value.Parameter @Override public abstract ImList.Immutable<String> scopeNames();

    @Value.Parameter @Override public abstract Optional<Rule> rule();

    @Value.Parameter @Override public abstract boolean changed();

    @Value.Parameter @Override public abstract Map.Immutable<String, IStatixGroup> groups();

    @Value.Parameter @Override public abstract Map.Immutable<String, IStatixUnit> units();

    @Override public String toString() {
        return "StatixGroup@" + System.identityHashCode(this);
    }

}
