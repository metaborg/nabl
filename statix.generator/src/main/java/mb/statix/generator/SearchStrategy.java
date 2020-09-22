package mb.statix.generator;

import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

public abstract class SearchStrategy<I, O> {

    public enum Mode {
        ENUM, RND
    }

    public SearchStrategy() {

    }


    public final SearchNodes<O> apply(SearchContext ctx, SearchNode<I> node) {
        return doApply(ctx, node);
    }

    protected abstract SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node);

}