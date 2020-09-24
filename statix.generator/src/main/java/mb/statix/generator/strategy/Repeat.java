package mb.statix.generator.strategy;

import java.util.Iterator;
import java.util.stream.Stream;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.generator.util.StreamUtil;

public final class Repeat<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;

    Repeat(SearchStrategy<I, O> s) {
        this.s = s;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        final Stream<SearchNode<O>> nodes = StreamUtil.generate(() -> true, () -> {
            Iterator<SearchNode<O>> it = null;
            while (it == null || !it.hasNext()) {
                it = s.apply(ctx, node).nodes().iterator();
            }
            return it.next();
        });
        return SearchNodes.of(node, this::toString, nodes);
    }

    @Override public String toString() {
        return "repeat(" + s.toString() + ")";
    }

}