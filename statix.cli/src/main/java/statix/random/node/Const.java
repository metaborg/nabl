package statix.random.node;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.core.MetaborgException;

import statix.random.SearchNode;
import statix.random.SearchState;

public class Const extends SearchNode<SearchState, SearchState> {

    public Const(Random rnd) {
        super(rnd);
    }

    final AtomicBoolean fired = new AtomicBoolean();

    @Override protected void doInit() {
        fired.set(false);
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException {
        if(fired.getAndSet(true)) {
            return Optional.empty();
        }
        return Optional.of(input);
    }

    @Override public String toString() {
        return "const";
    }

}