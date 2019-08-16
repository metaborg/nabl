package statix.random;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.core.MetaborgException;

public abstract class SearchNode<I, O> {

    private final AtomicBoolean init = new AtomicBoolean(false);

    protected final Random rnd;

    public SearchNode(Random rnd) {
        this.rnd = rnd;
    }

    protected I input;

    public void init(I i) {
        init.set(true);
        this.input = i;
        doInit();
    }

    protected abstract void doInit();

    public Optional<O> next() throws MetaborgException {
        if(!init.get()) {
            throw new IllegalStateException();
        }
        return doNext();
    }

    protected abstract Optional<O> doNext() throws MetaborgException;

    protected <E> E pick(Set<E> set) {
        final int index = rnd.nextInt(set.size());
        final Iterator<E> iter = set.iterator();
        for(int i = 0; i < index; i++) {
            iter.next();
        }
        final E element = iter.next();
        iter.remove();
        return element;
    }

}