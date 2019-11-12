package mb.statix.generator.strategy;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.spec.Spec;

final class Identity<I extends SearchState> extends SearchStrategy<I, I> {

    Identity(Spec spec) {
        super(spec);
    }

    @Override protected SearchNodes<I> doApply(SearchContext ctx, SearchNode<I> node) {
        return SearchNodes.of(node, this::toString, node);
    }

    @Override public String toString() {
        return "id";
    }

}