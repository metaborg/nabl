package mb.nabl2.scopegraph.terms.path;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.spoofax.terms.util.NotImplementedException;

import com.google.common.collect.Queues;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IScopePath;
import mb.nabl2.scopegraph.path.IStep;

public class PathIterator<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements Iterator<IStep<S, L, O>> {

    private IStep<S, L, O> next;
    private Deque<IScopePath<S, L, O>> stack = Queues.newArrayDeque();

    public PathIterator(IScopePath<S, L, O> path) {
        stack.push(path);
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
        IStep<S, L, O> step = next;
        next = null;
        return step;
    }

    private void findNext() {
        while(next == null && !stack.isEmpty()) {
            findNext(stack.pop());
        }
    }

    private boolean findNext(IScopePath<S, L, O> nexts) {
        if(nexts instanceof EStep || nexts instanceof NStep) {
            next = (IStep<S, L, O>) nexts;
            return true;
        } else if(nexts instanceof EmptyScopePath) {
            return false;
        } else if(nexts instanceof ComposedScopePath) {
            final ComposedScopePath<S, L, O> comp = (ComposedScopePath<S, L, O>) nexts;
            stack.push(comp.getRight());
            if(!findNext(comp.getLeft())) {
                return findNext(stack.pop()); // this should be comp.right!
            }
            return true;
        } else {
            throw new NotImplementedException("Missing case for " + nexts.getClass());
        }
    }

}
