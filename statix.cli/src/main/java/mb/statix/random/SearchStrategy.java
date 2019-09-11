package mb.statix.random;

import java.util.Optional;
import java.util.stream.Stream;

import mb.nabl2.util.Tuple2;

public abstract class SearchStrategy<I, O> {

    public final Stream<SearchNode<O>> apply(SearchContext ctx, int size, I input, SearchNode<?> parent) {
        return doApply(ctx, size, input, parent);
    }

    protected abstract Stream<SearchNode<O>> doApply(SearchContext ctx, int size, I input, SearchNode<?> parent);

    public Tuple2<Stream<SearchNode<O>>, Optional<SearchStrategy<O, ?>>> applyRec(SearchContext ctx, int size,
            I input, SearchNode<?> parent) {
        return null;
    }

}