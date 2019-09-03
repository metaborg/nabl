package mb.statix.random.node;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.core.MetaborgException;

import mb.statix.random.SearchNode;

public class Seq<I, X, O> extends SearchNode<I, O> {

    private final SearchNode<I, X> n1;
    private final SearchNode<X, O> n2;

    public Seq(Random rnd, SearchNode<I, X> n1, SearchNode<X, O> n2) {
        super(rnd);
        this.n1 = n1;
        this.n2 = n2;
    }

    private final AtomicBoolean mustStep = new AtomicBoolean();

    @Override protected void doInit() {
        n1.init(input);
        mustStep.set(true);
    }

    @Override protected Optional<O> doNext() throws MetaborgException {
        if(mustStep.getAndSet(false)) {
            final Optional<X> result1 = n1.next();
            if(!result1.isPresent()) {
                return Optional.empty();
            }
            n2.init(result1.get());
        }
        final Optional<O> result2 = n2.next();
        if(!result2.isPresent()) {
            mustStep.set(true);
            return doNext();
        }
        return result2;
    }

    @Override public String toString() {
        return n1.toString() + "; " + n2.toString();
    }

}