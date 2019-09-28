package mb.statix.random.strategy;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class For<I, O> extends SearchStrategy<I, O> {

    private final SearchStrategy<I, O> s;
    private final int n;

    For(int n, SearchStrategy<I, O> s) {
        this.s = s;
        this.n = n;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        final Stream<SearchNode<O>> nodes = IntStream.range(0, n).boxed().flatMap(i -> {
            return s.apply(ctx, input, parent).nodes();
        });
        return SearchNodes.of(parent, this.toString(), nodes);
    }

    @Override public String toString() {
        return "for(" + n + ", " + s.toString() + ")";
    }

}