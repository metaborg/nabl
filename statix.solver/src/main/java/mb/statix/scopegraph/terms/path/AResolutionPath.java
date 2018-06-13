package mb.statix.scopegraph.terms.path;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AResolutionPath<V, L, R> implements IResolutionPath<V, L, R> {

    @Value.Parameter @Override public abstract IScopePath<V, L> getPath();

    @Value.Parameter @Override public abstract Optional<R> getRelation();

    @Value.Parameter @Override public abstract List<V> getDatum();

    @Value.Check public @Nullable AResolutionPath<V, L, R> check() {
        return this;
    }

    @Value.Lazy @Override public Set.Immutable<V> getScopes() {
        return getPath().getScopes();
    }

    @Value.Lazy @Override public PSequence<L> getLabels() {
        return getPath().getLabels();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPath());
        sb.append(Paths.PATH_SEPARATOR);
        sb.append(getRelation());
        sb.append(Paths.PATH_SEPARATOR);
        sb.append(getDatum());
        return sb.toString();
    }

}