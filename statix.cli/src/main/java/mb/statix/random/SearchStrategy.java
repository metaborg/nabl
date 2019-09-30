package mb.statix.random;

import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

public abstract class SearchStrategy<I, O> {

    public final SearchNodes<O> apply(SearchContext ctx, SearchNode<I> node) {
        return doApply(ctx, node);
    }

    protected abstract SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node);

}