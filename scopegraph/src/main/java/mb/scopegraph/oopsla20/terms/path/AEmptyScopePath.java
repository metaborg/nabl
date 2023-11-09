package mb.scopegraph.oopsla20.terms.path;

import java.util.Collections;
import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ConsList;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.path.IScopePath;
import mb.scopegraph.oopsla20.path.IStep;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AEmptyScopePath<S, L> implements IScopePath<S, L> {

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

    @Value.Lazy @Override public ConsList<S> scopes() {
        return ConsList.of(getScope());
    }

    @Value.Lazy @Override public Set.Immutable<S> scopeSet() {
        return CapsuleUtil.immutableSet(getScope());
    }

    @Value.Lazy @Override public ConsList<L> labels() {
        return ConsList.of();
    }

    @Override public Iterator<IStep<S, L>> iterator() {
        return Collections.emptyIterator();
    }

    @Override public int hashCode() {
        return 0;
    }

    @Override public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(!(obj instanceof IScopePath<?, ?>))
            return false;
        IScopePath<?, ?> other = (IScopePath<?, ?>) obj;
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