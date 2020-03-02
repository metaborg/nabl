package mb.statix.generator.strategy;

import mb.statix.generator.EitherSearchState;
import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

public final class Match<I1 extends SearchState, I2 extends SearchState, O extends SearchState>
        extends SearchStrategy<EitherSearchState<I1, I2>, O> {
    private final SearchStrategy<I2, O> s2;
    private final SearchStrategy<I1, O> s1;

    Match(SearchStrategy<I1, O> s1, SearchStrategy<I2, O> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected SearchNodes<O> doApply(SearchContext ctx, SearchNode<EitherSearchState<I1, I2>> node) {
        final EitherSearchState<I1, I2> input = node.output();
        return input.map(
                i1 -> s1.apply(ctx, new SearchNode<>(node.id(), i1, node.parent(), node.desc())),
                i2 -> s2.apply(ctx, new SearchNode<>(node.id(), i2, node.parent(), node.desc()))
        );
    }

    @Override public String toString() {
        return ">(" + s1 + " | " + s2 + ")";
    }
}