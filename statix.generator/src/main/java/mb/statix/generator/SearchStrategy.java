package mb.statix.generator;

import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.spec.Spec;

public abstract class SearchStrategy<I extends SearchState, O extends SearchState> {

    public enum Mode {
        ENUM, RND
    }

    private final Spec spec;

    public SearchStrategy(Spec spec) {
        this.spec = spec;
    }

    protected Spec spec() {
        return this.spec;
    }

    public final SearchNodes<O> apply(SearchContext ctx, SearchNode<I> node) {
        return doApply(ctx, node);
    }

    protected abstract SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node);

}