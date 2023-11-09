package mb.scopegraph.oopsla20.terms.path;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ConsList;
import org.metaborg.util.iterators.Iterators2;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.path.IStep;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AStep<S, L> implements IStep<S, L> {

    @Value.Parameter @Override public abstract S getSource();

    @Value.Parameter @Override public abstract L getLabel();

    @Value.Parameter @Override public abstract S getTarget();

    @Value.Lazy @Override public int size() {
        return 1;
    }

    @Value.Lazy @Override public ConsList<S> scopes() {
        return ConsList.of(getSource(), getTarget());
    }

    @Value.Lazy @Override public Set.Immutable<S> scopeSet() {
        return CapsuleUtil.immutableSet(getSource(), getTarget());
    }

    @Value.Lazy @Override public ConsList<L> labels() {
        return ConsList.of(getLabel());
    }

    @Override public Iterator<IStep<S, L>> iterator() {
        return Iterators2.singleton(this);
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