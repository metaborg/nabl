package mb.scopegraph.pepm16.terms.path;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Objects;

import jakarta.annotation.Nullable;

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
abstract class AComposedScopePath<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IScopePath<S, L, O> {

    @Value.Parameter public abstract IScopePath<S, L, O> getLeft();

    @Value.Parameter public abstract IScopePath<S, L, O> getRight();

    @Value.Check public @Nullable AComposedScopePath<S, L, O> check() {
        // left and right are not connected
        if(!getLeft().getTarget().equals(getRight().getSource())) {
            return null;
        }
        // path is cyclic
        if(getScopes().size() <= size()) {
            return null;
        }
        return this;
    }

    @Value.Lazy @Override public S getSource() {
        return getLeft().getSource();
    }

    @Value.Lazy @Override public S getTarget() {
        return getRight().getTarget();
    }

    @Value.Lazy @Override public int size() {
        return getLeft().size() + getRight().size();
    }

    @Value.Lazy @Override public Set.Immutable<O> getImports() {
        return getLeft().getImports().__insertAll(getRight().getImports());
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables2.fromConcat(getLeft().getImportPaths(), getRight().getImportPaths());
    }

    @Value.Lazy @Override public Set.Immutable<S> getScopes() {
        return getLeft().getScopes().__insertAll(getRight().getScopes());
    }

    @Value.Lazy @Override public ConsList<L> getLabels() {
        return getLeft().getLabels().append(getRight().getLabels());
    }

    @Value.Lazy @Override public int hashCode() {
        return getLeft().hashCode() + (BigInteger.valueOf(31).pow(getLeft().size()).intValue() * getRight().hashCode());
    }

    @Override public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(!(obj instanceof IScopePath<?, ?, ?>))
            return false;
        IScopePath<?, ?, ?> other = (IScopePath<?, ?, ?>) obj;
        if(!getSource().equals(other.getSource()))
            return false;
        if(!getTarget().equals(other.getTarget()))
            return false;
        Iterator<?> iterator1 = this.iterator();
        Iterator<?> iterator2 = other.iterator();
        while(iterator1.hasNext()) {
            if(!iterator2.hasNext()) {
                return false;
            }
            Object o1 = iterator1.next();
            Object o2 = iterator2.next();
            if(!Objects.equals(o1, o2)) {
                return false;
            }
        }
        return !iterator2.hasNext();
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        StringBuilder sb = new StringBuilder();
        sb.append(getLeft().toString(includeSource, true));
        sb.append(Paths.PATH_SEPERATOR);
        sb.append(getRight().toString(false, includeTarget));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(true, true);
    }

}
