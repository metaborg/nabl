package mb.scopegraph.pepm16.terms.path;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ConsList;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.path.IScopePath;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AEmptyScopePath<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IScopePath<S, L, O> {

    @Value.Parameter public abstract S getScope();

    @Override public S getSource() {
        return getScope();
    }

    @Override public S getTarget() {
        return getScope();
    }

    @Value.Lazy @Override public int size() {
        return 0;
    }

    @Value.Lazy @Override public Set.Immutable<O> getImports() {
        return Set.Immutable.of();
    }

    @Value.Lazy @Override public Set.Immutable<S> getScopes() {
        return Set.Immutable.of(getScope());
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables2.empty();
    }

    @Value.Lazy @Override public ConsList<L> getLabels() {
        return ConsList.of();
    }

    @Override public int hashCode() {
        return 0;
    }

    @Override public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(!(obj instanceof IScopePath<?, ?, ?>))
            return false;
        IScopePath<?, ?, ?> other = (IScopePath<?, ?, ?>) obj;
        if(!getScope().equals(other.getSource()))
            return false;
        if(!getScope().equals(other.getTarget()))
            return false;
        return other.size() == 0;
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        return (includeSource && includeTarget) ? getScope().toString() : "";
    }

    @Override public String toString() {
        return toString(true, true);
    }

}