package mb.statix.random.strategy;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Identity<I> extends SearchStrategy<I, I> {
    @Override protected SearchNodes<I> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        return SearchNodes.of(parent, this.toString(),
                new SearchNode<>(ctx.nextNodeId(), input, parent, this.toString()));
    }

    @Override public String toString() {
        return "id";
    }
}