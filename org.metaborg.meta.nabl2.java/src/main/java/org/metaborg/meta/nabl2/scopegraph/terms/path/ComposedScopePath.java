package org.metaborg.meta.nabl2.scopegraph.terms.path;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IStep;
import org.metaborg.meta.nabl2.util.collections.PSets;
import org.pcollections.PSequence;
import org.pcollections.PSet;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ComposedScopePath<S extends IScope, L extends ILabel, O extends IOccurrence>
    implements IScopePath<S, L, O> {

    @Value.Parameter public abstract IScopePath<S, L, O> getLeft();

    @Value.Parameter public abstract IScopePath<S, L, O> getRight();

    @Value.Check public @Nullable ComposedScopePath<S, L, O> check() {
        if(!getLeft().getTarget().equals(getRight().getSource())) {
            return null;
        }
        if(PSets.intersect(getLeft().getScopes(), getLeft().getTarget(), getRight().getScopes())) {
            return null;
        }
        return this;
    }

    @Override public S getSource() {
        return getLeft().getSource();
    }

    @Override public S getTarget() {
        return getRight().getTarget();
    }

    @Value.Lazy @Override public PSet<O> getImports() {
        return getLeft().getImports().plusAll(getRight().getImports());
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables.concat(getLeft().getImportPaths(), getRight().getImportPaths());
    }

    @Value.Lazy @Override public PSet<S> getScopes() {
        return getLeft().getScopes().plusAll(getRight().getScopes());
    }

    @Value.Lazy @Override public PSequence<L> getLabels() {
        return getLeft().getLabels().plusAll(getRight().getLabels());
    }

    @Override public Iterator<IStep<S, L, O>> iterator() {
        return Iterators.concat(getLeft().iterator(), getRight().iterator());
    }

    @Override public int hashCode() {
        int result = 1;
        for(IStep<S, L, O> step : this) {
            result = 31 * result + step.hashCode();
        }
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(!(obj instanceof IScopePath<?, ?, ?>))
            return false;
        IScopePath<?, ?, ?> other = (IScopePath<?, ?, ?>) obj;
        return Iterators.elementsEqual(this.iterator(), other.iterator());
    }

}