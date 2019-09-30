package mb.statix.random.strategy;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.Either2;

final class Match<I1, I2, O> extends SearchStrategy<Either2<I1, I2>, O> {
    private final SearchStrategy<I2, O> s2;
    private final SearchStrategy<I1, O> s1;

    Match(SearchStrategy<I1, O> s1, SearchStrategy<I2, O> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected SearchNodes<O> doApply(SearchContext ctx, SearchNode<Either2<I1, I2>> node) {
        final Either2<I1, I2> input = node.output();
        return input.map(i1 -> {
            return s1.apply(ctx, new SearchNode<>(node.id(), i1, node.parent(), node.desc()));
        }, i2 -> {
            return s2.apply(ctx, new SearchNode<>(node.id(), i2, node.parent(), node.desc()));
        });
    }

    @Override public String toString() {
        return ">(" + s1 + " | " + s2 + ")";
    }
}