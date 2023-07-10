package mb.scopegraph.pepm16.path;

import java.util.Iterator;

import org.metaborg.util.collection.ImmutableCollection;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.terms.path.PathIterator;

public interface IScopePath<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends IPath<S, L, O>, ImmutableCollection<IStep<S, L, O>> {

    S getSource();

    S getTarget();

    int size();

    String toString(boolean includeSource, boolean includeTarget);

    @Override default Iterator<IStep<S, L, O>> iterator() {
        return new PathIterator<>(this);
    }

}