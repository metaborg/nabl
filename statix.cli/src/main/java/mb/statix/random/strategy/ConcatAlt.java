package mb.statix.random.strategy;

import java.util.stream.Stream;

import org.metaborg.util.functions.Function0;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.Either2;

final class ConcatAlt<I, O1, O2> extends SearchStrategy<I, Either2<O1, O2>> {
    private final SearchStrategy<I, O2> s2;
    private final SearchStrategy<I, O1> s1;

    ConcatAlt(SearchStrategy<I, O1> s1, SearchStrategy<I, O2> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected SearchNodes<Either2<O1, O2>> doApply(SearchContext ctx, SearchNode<I> node) {
        final SearchNodes<O1> sn1 = s1.apply(ctx, node);
        final SearchNodes<O2> sn2 = s2.apply(ctx, node);
        final Stream<SearchNode<Either2<O1, O2>>> nodes =
                Stream.concat(sn1.nodes().map(n1 -> output1(ctx, n1)), sn2.nodes().map(n2 -> output2(ctx, n2)));
        final Function0<String> desc = () -> "( " + sn1.desc() + " | " + sn2.desc() + " )<";
        return SearchNodes.of(node, desc, nodes);
    }

    private SearchNode<Either2<O1, O2>> output1(SearchContext ctx, final SearchNode<O1> n1) {
        final SearchNode<Either2<O1, O2>> next;
        final Either2<O1, O2> output = Either2.ofLeft(n1.output());
        next = new SearchNode<>(ctx.nextNodeId(), output, n1.parent(), n1.desc());
        return next;
    }

    private SearchNode<Either2<O1, O2>> output2(SearchContext ctx, final SearchNode<O2> n2) {
        final SearchNode<Either2<O1, O2>> next;
        final Either2<O1, O2> output = Either2.ofRight(n2.output());
        next = new SearchNode<>(ctx.nextNodeId(), output, n2.parent(), n2.desc());
        return next;
    }

    @Override public String toString() {
        return "(" + s1 + " | " + s2 + ")<";
    }

}