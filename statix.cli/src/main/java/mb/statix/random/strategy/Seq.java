package mb.statix.random.strategy;

import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchStrategy;

final class Seq<I1, O, I2> extends SearchStrategy<I1, O> {
    private final SearchStrategy<I1, I2> s1;
    private final SearchStrategy<I2, O> s2;

    Seq(SearchStrategy<I1, I2> s1, SearchStrategy<I2, O> s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    @Override public Stream<SearchNode<O>> doApply(SearchContext ctx, I1 i1, SearchNode<?> parent) {
        return s1.apply(ctx, i1, parent).flatMap(sn1 -> {
            return s2.apply(ctx, sn1.output(), sn1).map(sn2 -> {
                return new SearchNode<>(ctx.nextNodeId(), sn2.output(), sn2,
                        "(" + sn1.toString() + " . " + sn2.toString() + ")");
            });
        });
    }

    @Override public String toString() {
        return "(" + s1.toString() + " . " + s2.toString() + ")";
    }
}