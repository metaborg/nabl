package mb.statix.generator.strategy;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

public final class Identity<I extends SearchState> extends SearchStrategy<I, I> {

    @Override protected SearchNodes<I> doApply(SearchContext ctx, SearchNode<I> node) {
        return SearchNodes.of(node, this::toString, node);
    }

    @Override public String toString() {
        return "id";
    }

}