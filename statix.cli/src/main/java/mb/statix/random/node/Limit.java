package mb.statix.random.node;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.core.MetaborgException;

import mb.statix.random.SearchNode;

public class Limit<I, O> extends SearchNode<I, O> {

    private final int limit;
    private final SearchNode<I, O> n;

    public Limit(Random rnd, int limit, SearchNode<I, O> n) {
        super(rnd);
        this.limit = limit;
        this.n = n;
    }

    private final AtomicInteger remaining = new AtomicInteger();

    @Override protected void doInit() {
        n.init(input);
        remaining.set(limit);
    }

    @Override protected Optional<O> doNext() throws MetaborgException {
        if(remaining.getAndDecrement() <= 0) {
            return Optional.empty();
        }
        return n.next();
    }

    @Override public String toString() {
        return "limit(" + limit + ", " + n.toString() + ")";
    }

}