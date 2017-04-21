package org.metaborg.meta.nabl2.scopegraph.terms.path;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IStep;
import org.metaborg.util.iterators.Iterables2;
import org.pcollections.HashTreePSet;
import org.pcollections.PSequence;
import org.pcollections.PSet;
import org.pcollections.TreePVector;

import com.google.common.collect.Iterators;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class EmptyScopePath<S extends IScope, L extends ILabel, O extends IOccurrence>
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

    @Value.Lazy @Override public PSet<O> getImports() {
        return HashTreePSet.empty();
    }

    @Value.Lazy @Override public PSet<S> getScopes() {
        return HashTreePSet.singleton(getScope());
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables2.empty();
    }

    @Value.Lazy @Override public PSequence<L> getLabels() {
        return TreePVector.empty();
    }

    @Override public Iterator<IStep<S, L, O>> iterator() {
        return Iterators.emptyIterator();
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
        return Iterators.size(other.iterator()) == 0;
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        return (includeSource && includeTarget) ? getScope().toString() : "";
    }

    @Override public String toString() {
        return toString(true, true);
    }

}