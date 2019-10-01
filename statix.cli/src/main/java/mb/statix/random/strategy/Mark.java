package mb.statix.random.strategy;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Mark<I extends SearchState> extends SearchStrategy<I, I> {

    private final String marker;

    public Mark(String marker) {
        this.marker = "<" + marker + ">";
    }

    @Override protected SearchNodes<I> doApply(SearchContext ctx, SearchNode<I> node) {
        return SearchNodes.of(node, this::toString, new SearchNode<>(ctx.nextNodeId(), node.output(), node, marker));
    }

    @Override public String toString() {
        return marker;
    }

}