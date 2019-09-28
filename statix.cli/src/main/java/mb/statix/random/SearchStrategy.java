package mb.statix.random;

import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

public abstract class SearchStrategy<I, O> {

    public final SearchNodes<O> apply(SearchContext ctx, I input, SearchNode<?> parent) {
        return doApply(ctx, input, parent);
    }

    protected abstract SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent);

}