package mb.statix.scopegraph.terms.path;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.Iterators;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;
import mb.statix.scopegraph.path.IStep;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AStep<V, L> implements IStep<V, L> {

    @Value.Parameter @Override public abstract V getSource();

    @Value.Parameter @Override public abstract L getLabel();

    @Value.Parameter @Override public abstract V getTarget();

    @Value.Lazy @Override public int size() {
        return 1;
    }

    @Value.Lazy @Override public Set.Immutable<V> getScopes() {
        return Set.Immutable.of(getSource(), getTarget());
    }

    @Value.Lazy @Override public PSequence<L> getLabels() {
        return PSequence.of(getLabel());
    }

    @Override public Iterator<IStep<V, L>> iterator() {
        return Iterators.singletonIterator(this);
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        StringBuilder sb = new StringBuilder();
        if(includeSource) {
            sb.append(getSource());
            sb.append(Paths.PATH_SEPARATOR);
        }
        sb.append(getLabel());
        if(includeTarget) {
            sb.append(Paths.PATH_SEPARATOR);
            sb.append(getTarget());
        }
        return sb.toString();
    }

    @Override public String toString() {
        return toString(true, true);
    }

}