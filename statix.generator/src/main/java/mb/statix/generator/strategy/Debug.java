package mb.statix.generator.strategy;

import org.metaborg.util.functions.Action1;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

public final class Debug<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {
    private final Action1<SearchNode<O>> debug;
    private final SearchStrategy<I, O> s;

    Debug(Action1<SearchNode<O>> debug, SearchStrategy<I, O> s) {
        this.debug = debug;
        this.s = s;
    }

    @Override protected SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        return s.apply(ctx, node).map(n -> {
            debug.apply(n);
            return n;
        });
    }

    @Override public String toString() {
        return s.toString();
    }
}