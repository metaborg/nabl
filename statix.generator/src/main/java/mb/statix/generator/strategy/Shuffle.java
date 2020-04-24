package mb.statix.generator.strategy;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Shuffles the resulting nodes randomly.
 *
 * @param <I> the type of input
 * @param <O> the type of output
 */
public final class Shuffle<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {

    private final SearchStrategy<I, O> s;

    Shuffle(SearchStrategy<I, O> s) {
        this.s = s;
    }

    @Override protected SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        SearchNodes<O> result = s.apply(ctx, node);
        List<SearchNode<O>> nodes = result.nodes().collect(Collectors.toList());
        Collections.shuffle(nodes, ctx.rnd());

        return SearchNodes.of(node, this::toString, nodes);
    }

    @Override public String toString() {
        return "shuffle(" + s + ")";
    }

}