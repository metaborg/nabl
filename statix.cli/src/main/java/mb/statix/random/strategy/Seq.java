package mb.statix.random.strategy;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Seq<I1, O, I2> extends SearchStrategy<I1, O> {
    private final SearchStrategy<I1, I2> s1;
    private final SearchStrategy<I2, O> s2;

    Seq(SearchStrategy<I1, I2> s1, SearchStrategy<I2, O> s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    @Override public SearchNodes<O> doApply(SearchContext ctx, I1 i1, SearchNode<?> parent) {
        return s1.apply(ctx, i1, parent).flatMap(n -> s2.apply(ctx, n.output(), n));
    }

    @Override public String toString() {
        return "(" + s1.toString() + " . " + s2.toString() + ")";
    }

}