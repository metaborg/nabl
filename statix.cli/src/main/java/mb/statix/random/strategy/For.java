package mb.statix.random.strategy;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class For<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {

    private final SearchStrategy<I, O> s;
    private final int n;

    For(int n, SearchStrategy<I, O> s) {
        this.s = s;
        this.n = n;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        final Stream<SearchNode<O>> nodes = IntStream.range(0, n).boxed().flatMap(i -> {
            return s.apply(ctx, node).nodes();
        });
        return SearchNodes.of(node, this::toString, nodes);
    }

    @Override public String toString() {
        return "for(" + n + ", " + s.toString() + ")";
    }

}