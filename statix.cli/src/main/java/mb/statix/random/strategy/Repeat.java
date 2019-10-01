package mb.statix.random.strategy;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.StreamUtil;

final class Repeat<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {
    private final SearchStrategy<I, O> s;

    Repeat(SearchStrategy<I, O> s) {
        this.s = s;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        AtomicReference<Iterator<SearchNode<O>>> ns = new AtomicReference<>();
        final Stream<SearchNode<O>> nodes = StreamUtil.generate(() -> true, () -> {
            Iterator<SearchNode<O>> it;
            while((it = ns.get()) == null || !it.hasNext()) {
                ns.set((it = s.apply(ctx, node).nodes().iterator()));
            }
            return it.next();
        });
        return SearchNodes.of(node, this::toString, nodes);
    }

    @Override public String toString() {
        return "repeat(" + s.toString() + ")";
    }

}