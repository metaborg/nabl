package mb.statix.random.strategy;

import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Repeat<I, O> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;

    Repeat(SearchStrategy<I, O> s) {
        this.s = s;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        final Stream<SearchNode<O>> nodes = Stream.generate(() -> s.apply(ctx, input, parent).nodes()).flatMap(n -> n);
        return SearchNodes.of(parent, this.toString(), nodes);
    }

    @Override public String toString() {
        return "repeat(" + s.toString() + ")";
    }

}