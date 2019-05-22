package mb.statix.scopegraph.terms.path;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.Iterators;
import com.google.common.math.IntMath;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.path.IStep;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AComposedScopePath<S, L> implements IScopePath<S, L> {

    @Value.Parameter public abstract IScopePath<S, L> getLeft();

    @Value.Parameter public abstract IScopePath<S, L> getRight();

    @Value.Check public @Nullable AComposedScopePath<S, L> check() {
        // left and right are not connected
        if(!getLeft().getTarget().equals(getRight().getSource())) {
            return null;
        }
        // path is cyclic
        if(scopeSet().size() < size()) {
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

    @Value.Lazy @Override public PSequence<S> scopes() {
        return getLeft().scopes().appendAll(getRight().scopes().tail());
    }

    @Value.Lazy @Override public Set.Immutable<S> scopeSet() {
        return getLeft().scopeSet().__insertAll(getRight().scopeSet());
    }

    @Value.Lazy @Override public PSequence<L> labels() {
        return getLeft().labels().appendAll(getRight().labels());
    }

    @Override public Iterator<IStep<S, L>> iterator() {
        return Iterators.concat(getLeft().iterator(), getRight().iterator());
    }

    @Override public int hashCode() {
        return getLeft().hashCode() + (IntMath.pow(31, getLeft().size()) * getRight().hashCode());
    }

    @Override public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(!(obj instanceof IScopePath<?, ?>))
            return false;
        IScopePath<?, ?> other = (IScopePath<?, ?>) obj;
        if(!getSource().equals(other.getSource()))
            return false;
        if(!getTarget().equals(other.getTarget()))
            return false;
        return Iterators.elementsEqual(this.iterator(), other.iterator());
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        StringBuilder sb = new StringBuilder();
        sb.append(getLeft().toString(includeSource, true));
        sb.append(Paths.PATH_SEPARATOR);
        sb.append(getRight().toString(false, includeTarget));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(true, true);
    }

}
