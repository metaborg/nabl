package mb.statix.random.strategy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;

import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.util.StreamUtil;
import mb.statix.random.util.WeightedDrawSet;
import mb.statix.solver.IConstraint;

final class Select<C extends IConstraint> extends SearchStrategy<SearchState, FocusedSearchState<C>> {
    private final Class<C> cls;
    private final Function1<SearchState, Function1<C, Integer>> weight;

    Select(Class<C> cls, Function1<SearchState, Function1<C, Integer>> weight) {
        this.cls = cls;
        this.weight = weight;
    }

    @Override protected SearchNodes<FocusedSearchState<C>> doApply(SearchContext ctx, SearchNode<SearchState> node) {
        final SearchState input = node.output();
        final Function1<C, Integer> w = weight.apply(input);
        final List<Tuple2<C, Integer>> candidates = StreamUtil.filterInstances(cls, input.constraints().stream())
                .map(c -> ImmutableTuple2.of(c, w.apply(c))).collect(Collectors.toList());
        if(candidates.isEmpty()) {
            return SearchNodes.failure(node, this.toString() + "[no candidates]");
        }
        final WeightedDrawSet<C> candidateSet = new WeightedDrawSet<>(candidates);
        Stream<SearchNode<FocusedSearchState<C>>> nodes = candidateSet.enumerate(ctx.rnd()).map(c -> {
            final FocusedSearchState<C> output = FocusedSearchState.of(input, c.getKey());
            return new SearchNode<>(ctx.nextNodeId(), output, node,
                    "select(" + c.getKey().toString(t -> input.state().unifier().toString(t)) + ")");
        });
        final String desc = this.toString() + "[" + candidates.size() + "]";
        return SearchNodes.of(node, () -> desc, nodes);
    }

    @Override public String toString() {
        return "select(" + cls.getSimpleName() + ", " + weight.toString() + ")";
    }

}