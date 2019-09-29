package mb.statix.random.strategy;

import java.util.Iterator;

import com.google.common.collect.Streams;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Require<I, O> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;

    Require(SearchStrategy<I, O> s) {
        this.s = s;
    }

    @Override protected SearchNodes<O> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        SearchNodes<O> nodes = s.apply(ctx, input, parent);
        if(!nodes.success()) {
            return nodes;
        }
        Iterator<SearchNode<O>> it = nodes.nodes().iterator();
        if(!it.hasNext()) {
            return SearchNodes.failure(parent, "require[no results]");
        }
        return SearchNodes.of(parent, Streams.stream(it));
    }

    @Override public String toString() {
        return "require(" + s + ")";
    }

}