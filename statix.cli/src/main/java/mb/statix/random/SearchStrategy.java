package mb.statix.random;

public abstract class SearchStrategy<I, O> {

    public final SearchNodes<O> apply(SearchContext ctx, I input, SearchNode<?> parent) {
        return doApply(ctx, input, parent);
    }

    protected abstract SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent);

}