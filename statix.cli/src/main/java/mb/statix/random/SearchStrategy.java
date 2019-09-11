package mb.statix.random;

import java.util.stream.Stream;

public abstract class SearchStrategy<I, O> {

    public final Stream<SearchNode<O>> apply(SearchContext ctx, I input, SearchNode<?> parent) {
        return doApply(ctx, input, parent);
    }

    protected abstract Stream<SearchNode<O>> doApply(SearchContext ctx, I input, SearchNode<?> parent);

}