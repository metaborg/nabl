package mb.scopegraph.pepm16.terms.path;

import java.util.Iterator;
import java.util.NoSuchElementException;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IScopePath;
import mb.scopegraph.pepm16.path.IStep;

public class PathIterator<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements Iterator<IStep<S, L, O>> {

    private IStep<S, L, O> next;
    private IScopePath<S, L, O> path;

    public PathIterator(IScopePath<S, L, O> path) {
        this.path = path;
    }

    @Override public boolean hasNext() {
        findNext();
        return next != null;
    }

    @Override public IStep<S, L, O> next() {
        findNext();
        if(next == null) {
            throw new NoSuchElementException();
        }
        IStep<S, L, O> result = next;
        next = null;
        return result;
    }

    private void findNext() {
        if(next != null) {
            return;
        }
        final IScopePath<S, L, O> p;
        if(path instanceof EmptyScopePath) {
            p = null;
            path = null;
        } else if(path instanceof ComposedScopePath) {
            final ComposedScopePath<S, L, O> q = (ComposedScopePath<S, L, O>) path;
            p = q.getLeft();
            path = q.getRight();
        } else {
            p = path;
            path = null;
        }
        if(p == null) {
            return;
        } else if(!(p instanceof IStep)) {
            throw new IllegalStateException();
        } else {
            next = (IStep<S, L, O>) p;
        }
    }

}
