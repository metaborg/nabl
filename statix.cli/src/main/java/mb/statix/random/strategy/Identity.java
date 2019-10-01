package mb.statix.random.strategy;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Identity<I extends SearchState> extends SearchStrategy<I, I> {

    @Override protected SearchNodes<I> doApply(SearchContext ctx, SearchNode<I> node) {
        return SearchNodes.of(node, this::toString, node);
    }

    @Override public String toString() {
        return "id";
    }

}