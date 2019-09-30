package mb.statix.random.strategy;

import org.metaborg.util.functions.Action1;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Debug<I, O> extends SearchStrategy<I, O> {
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