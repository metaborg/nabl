package mb.statix.random.strategy;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.Either2;
import mb.statix.random.util.StreamUtil;

final class Alt<I, O1, O2> extends SearchStrategy<I, Either2<O1, O2>> {
    private final SearchStrategy<I, O2> s2;
    private final SearchStrategy<I, O1> s1;

    Alt(SearchStrategy<I, O1> s1, SearchStrategy<I, O2> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected SearchNodes<Either2<O1, O2>> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
        final SearchNodes<O1> ns1 = s1.apply(ctx, input, parent);
        final SearchNodes<O2> ns2 = s2.apply(ctx, input, parent);

        final Iterator<SearchNode<O1>> it1 = ns1.nodes().iterator();
        final Iterator<SearchNode<O2>> it2 = ns2.nodes().iterator();

        Stream<SearchNode<Either2<O1, O2>>> nodes = StreamUtil.generate(() -> it1.hasNext() || it2.hasNext(), () -> {
            final boolean left = ctx.rnd().nextBoolean();
            final SearchNode<Either2<O1, O2>> next;
            if(left) {
                if(it2.hasNext()) {
                    final SearchNode<O2> n2 = it2.next();
                    final Either2<O1, O2> output = Either2.ofRight(n2.output());
                    next = new SearchNode<>(ctx.nextNodeId(), output, n2.parent(), n2.desc());
                } else if(it1.hasNext()) {
                    final SearchNode<O1> n1 = it1.next();
                    final Either2<O1, O2> output = Either2.ofLeft(n1.output());
                    next = new SearchNode<>(ctx.nextNodeId(), output, n1.parent(), n1.desc());
                } else {
                    throw new NoSuchElementException();
                }
            } else {
                if(it1.hasNext()) {
                    final SearchNode<O1> n1 = it1.next();
                    final Either2<O1, O2> output = Either2.ofLeft(n1.output());
                    next = new SearchNode<>(ctx.nextNodeId(), output, n1.parent(), n1.desc());
                } else if(it2.hasNext()) {
                    final SearchNode<O2> n2 = it2.next();
                    final Either2<O1, O2> output = Either2.ofRight(n2.output());
                    next = new SearchNode<>(ctx.nextNodeId(), output, n2.parent(), n2.desc());
                } else {
                    throw new NoSuchElementException();
                }
            }
            return next;
        });

        return SearchNodes.of(parent, nodes);
    }

    @Override public String toString() {
        return "(" + s1 + " | " + s2 + ")<";
    }

}