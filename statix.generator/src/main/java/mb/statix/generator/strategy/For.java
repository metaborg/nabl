package mb.statix.generator.strategy;

import static mb.statix.generator.util.StreamUtil.lazyFlatMap;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

public final class For<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {

    private final SearchStrategy<I, O> s;
    private final int n;

    For(int n, SearchStrategy<I, O> s) {
        this.s = s;
        this.n = n;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        final Stream<SearchNode<O>> nodes = lazyFlatMap(IntStream.range(0, n).boxed(),
                i -> s.apply(ctx, node).nodes());
        return SearchNodes.of(node, this::toString, nodes);
    }

    @Override public String toString() {
        return "for(" + n + ", " + s.toString() + ")";
    }

}