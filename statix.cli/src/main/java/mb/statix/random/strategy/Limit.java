package mb.statix.random.strategy;

import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchStrategy;

final class Limit<I, O> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;
    private final int n;

    Limit(int n, SearchStrategy<I, O> s) {
        this.s = s;
        this.n = n;
    }

    @Override public Stream<SearchNode<O>> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        return s.apply(ctx, input, parent).limit(n);
    }

    @Override public String toString() {
        return "limit(" + n + ", " + s.toString() + ")";
    }
}