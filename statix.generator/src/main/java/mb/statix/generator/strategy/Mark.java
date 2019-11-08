package mb.statix.generator.strategy;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

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