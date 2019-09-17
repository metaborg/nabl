package mb.statix.random.strategy;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchStrategy;

final class Alt<I, O1, O2> extends SearchStrategy<I, Either2<O1, O2>> {
    private final SearchStrategy<I, O2> s2;
    private final SearchStrategy<I, O1> s1;

    Alt(SearchStrategy<I, O1> s1, SearchStrategy<I, O2> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected Stream<SearchNode<Either2<O1, O2>>> doApply(SearchContext ctx, I input,
            SearchNode<?> parent) {
        final Iterator<SearchNode<O1>> ns1 = s1.apply(ctx, input, parent).iterator();
        final Iterator<SearchNode<O2>> ns2 = s2.apply(ctx, input, parent).iterator();
        return Streams.stream(new Iterator<SearchNode<Either2<O1, O2>>>() {

            @Override public boolean hasNext() {
                return ns1.hasNext() || ns2.hasNext();
            }

            @Override public SearchNode<Either2<O1, O2>> next() {
                final boolean left;
                if(ns1.hasNext() && ns2.hasNext()) {
                    left = ctx.rnd().nextBoolean();
                } else if(ns1.hasNext()) {
                    left = true;
                } else if(ns2.hasNext()) {
                    left = false;
                } else {
                    throw new NoSuchElementException();
                }
                if(left) {
                    final SearchNode<O1> n = ns1.next();
                    final Either2<O1, O2> output = Either2.ofLeft(n.output());
                    return new SearchNode<>(ctx.nextNodeId(), output, n.parent(), n.desc());
                } else {
                    final SearchNode<O2> n = ns2.next();
                    final Either2<O1, O2> output = Either2.ofRight(n.output());
                    return new SearchNode<>(ctx.nextNodeId(), output, n.parent(), n.desc());
                }
            }

        });

    }

    @Override public String toString() {
        return "(" + s1 + " | " + s2 + ")<";

    }
}