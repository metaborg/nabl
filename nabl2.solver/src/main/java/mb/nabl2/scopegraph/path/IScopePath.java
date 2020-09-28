package mb.nabl2.scopegraph.path;

import java.util.Iterator;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.terms.path.PathIterator;

public interface IScopePath<S extends IScope, L extends ILabel, O extends IOccurrence>
        extends IPath<S, L, O>, Iterable<IStep<S, L, O>> {

    S getSource();

    S getTarget();

    int size();

    String toString(boolean includeSource, boolean includeTarget);

    @Override default Iterator<IStep<S, L, O>> iterator() {
        return new PathIterator<>(this);
    }

    /**
     * Get first label of this path, or the given default.
     * 
     * Optimization for BUFirstStepComparator, so it doesn't need to create an iterator.
     */
    L getFirstLabel(L dataLabel);

}