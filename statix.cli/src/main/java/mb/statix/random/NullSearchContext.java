package mb.statix.random;

import java.util.Random;

public class NullSearchContext implements SearchContext {

    private final Random rnd;

    public NullSearchContext(Random rnd) {
        this.rnd = rnd;
    }

    @Override public Random rnd() {
        return rnd;
    }

    @Override public void addFailed(SearchNode<SearchState> node) {
        // ignore
    }

}