package mb.statix.generator.strategy;

import static mb.statix.generator.util.StreamUtil.lazyFlatMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Function0;

import mb.statix.generator.SearchContext;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;

public final class Concat<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {
    private final List<SearchStrategy<I, O>> ss;

    Concat(Iterable<SearchStrategy<I, O>> ss) {
        this.ss = ImList.Immutable.copyOf(ss);
    }

    @Override protected SearchNodes<O> doApply(SearchContext ctx, SearchNode<I> node) {
        final List<Function0<String>> descs = new ArrayList<>();
        final Stream<SearchNode<O>> nodes = lazyFlatMap(ss.stream(), s -> {
            final SearchNodes<O> sn = s.apply(ctx, node);
            descs.add(sn::desc);
            return sn.nodes();
        });
        final Function0<String> desc =
                () -> descs.stream().map(Function0::apply).collect(Collectors.joining(" ++ ", "(", ")"));
        return SearchNodes.of(node, desc, nodes);
    }

    @Override public String toString() {
        return ss.stream().map(Object::toString).collect(Collectors.joining(" ++ ", "(", ")"));
    }

}