package mb.statix.random.strategy;

import org.metaborg.util.functions.Function0;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Limit<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;
    private final int n;

    Limit(int n, SearchStrategy<I, O> s) {
        this.s = s;
        this.n = n;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        final SearchNodes<O> ns = s.apply(ctx, node);
        Function0<String> desc = () -> "limit(" + n + ", [" + ns.desc() + "])";
        return SearchNodes.of(node, desc, ns.nodes().limit(n));
    }

    @Override public String toString() {
        return "limit(" + n + ", " + s.toString() + ")";
    }

}