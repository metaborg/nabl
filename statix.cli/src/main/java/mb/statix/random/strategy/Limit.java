package mb.statix.random.strategy;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Limit<I, O> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;
    private final int n;

    Limit(int n, SearchStrategy<I, O> s) {
        this.s = s;
        this.n = n;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        final SearchNodes<O> ns = s.apply(ctx, input, parent);
        if(!ns.success()) {
            return ns;
        }
        return SearchNodes.of(parent, ns.nodes().limit(n));
    }

    @Override public String toString() {
        return "limit(" + n + ", " + s.toString() + ")";
    }
}