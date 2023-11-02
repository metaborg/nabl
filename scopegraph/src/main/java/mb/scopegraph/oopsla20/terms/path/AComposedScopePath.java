package mb.scopegraph.oopsla20.terms.path;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Objects;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ConsList;
import org.metaborg.util.iterators.Iterators2;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.path.IScopePath;
import mb.scopegraph.oopsla20.path.IStep;

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

    @Value.Lazy @Override public ConsList<S> scopes() {
        return getLeft().scopes().append(getRight().scopes().tail());
    }

    @Value.Lazy @Override public Set.Immutable<S> scopeSet() {
        return getLeft().scopeSet().__insertAll(getRight().scopeSet());
    }

    @Value.Lazy @Override public ConsList<L> labels() {
        return getLeft().labels().append(getRight().labels());
    }

    @Override public Iterator<IStep<S, L>> iterator() {
        return Iterators2.fromConcat(getLeft().iterator(), getRight().iterator());
    }

    @Override public int hashCode() {
        return getLeft().hashCode() + (BigInteger.valueOf(31).pow(getLeft().size()).intValue() * getRight().hashCode());
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
        sb.append(Paths.PATH_SEPARATOR);
        sb.append(getRight().toString(false, includeTarget));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(true, true);
    }

}
