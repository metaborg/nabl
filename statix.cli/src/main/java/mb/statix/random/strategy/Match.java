package mb.statix.random.strategy;

import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchStrategy;

final class Match<I1, I2, O> extends SearchStrategy<Either2<I1, I2>, O> {
    private final SearchStrategy<I2, O> s2;
    private final SearchStrategy<I1, O> s1;

    Match(SearchStrategy<I1, O> s1, SearchStrategy<I2, O> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected Stream<SearchNode<O>> doApply(SearchContext ctx, Either2<I1, I2> input,
            SearchNode<?> parent) {
        return input.map(n1 -> {
            return s1.apply(ctx, n1, parent);
        }, n2 -> {
            return s2.apply(ctx, n2, parent);
        });
    }

    @Override public String toString() {
        return ">(" + s1 + " | " + s2 + ")";
    }
}