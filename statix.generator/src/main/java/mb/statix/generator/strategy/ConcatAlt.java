package mb.statix.generator.strategy;

import java.util.stream.Stream;

import org.metaborg.util.functions.Function0;

import mb.statix.generator.EitherSearchState;
import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

final class ConcatAlt<I extends SearchState, O1 extends SearchState, O2 extends SearchState>
        extends SearchStrategy<I, EitherSearchState<O1, O2>> {
    private final SearchStrategy<I, O2> s2;
    private final SearchStrategy<I, O1> s1;

    ConcatAlt(SearchStrategy<I, O1> s1, SearchStrategy<I, O2> s2) {
        this.s2 = s2;
        this.s1 = s1;
    }

    @Override protected SearchNodes<EitherSearchState<O1, O2>> doApply(SearchContext ctx, SearchNode<I> node) {
        final SearchNodes<O1> sn1 = s1.apply(ctx, node);
        final SearchNodes<O2> sn2 = s2.apply(ctx, node);
        final Stream<SearchNode<EitherSearchState<O1, O2>>> nodes =
                Stream.concat(sn1.nodes().map(n1 -> output1(ctx, n1)), sn2.nodes().map(n2 -> output2(ctx, n2)));
        final Function0<String> desc = () -> "( " + sn1.desc() + " | " + sn2.desc() + " )<";
        return SearchNodes.of(node, desc, nodes);
    }

    private SearchNode<EitherSearchState<O1, O2>> output1(SearchContext ctx, final SearchNode<O1> n1) {
        final SearchNode<EitherSearchState<O1, O2>> next;
        final EitherSearchState<O1, O2> output = EitherSearchState.ofLeft(n1.output());
        next = new SearchNode<>(ctx.nextNodeId(), output, n1.parent(), n1.desc());
        return next;
    }

    private SearchNode<EitherSearchState<O1, O2>> output2(SearchContext ctx, final SearchNode<O2> n2) {
        final SearchNode<EitherSearchState<O1, O2>> next;
        final EitherSearchState<O1, O2> output = EitherSearchState.ofRight(n2.output());
        next = new SearchNode<>(ctx.nextNodeId(), output, n2.parent(), n2.desc());
        return next;
    }

    @Override public String toString() {
        return "(" + s1 + " | " + s2 + ")<";
    }

}