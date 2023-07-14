package mb.statix.generator.strategy;

import static mb.statix.generator.util.StreamUtil.lazyFlatMap;

import java.util.ArrayDeque;
import java.util.Deque;
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

public final class Seq<I extends SearchState, O extends SearchState> extends SearchStrategy<I, O> {

    private final List<SearchStrategy<?, ?>> ss;

    private Seq(List<SearchStrategy<?, ?>> ss) {
        this.ss = ss;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) @Override public SearchNodes<O> doApply(SearchContext ctx,
            SearchNode<I> node) {
        Stream<SearchNode> nodes = Stream.of(node);
        Deque<Function0<String>> descs = new ArrayDeque<>();
        for(SearchStrategy s : ss) {
            nodes = lazyFlatMap(nodes, n -> {
                final SearchNodes<?> sn = s.apply(ctx, n);
                descs.push(sn::desc);
                return sn.nodes();
            });
        }
        final Function0<String> desc =
                () -> descs.stream().map(Function0::apply).collect(Collectors.joining(" . ", "(", ")"));
        return SearchNodes.of(node, desc, (Stream) nodes);
    }

    @Override public String toString() {
        return ss.stream().map(Object::toString).collect(Collectors.joining(" . ", "(", ")"));
    }

    public static class Builder<I extends SearchState, O extends SearchState> {

        private final ImList.Mutable<SearchStrategy<?, ?>> ss = ImList.Mutable.of();

        public Builder(SearchStrategy<I, O> s) {
            ss.add(s);
        }

        @SuppressWarnings("unchecked") public <X extends SearchState> Builder<I, X> $(SearchStrategy<O, X> s) {
            ss.add(s);
            return (Builder<I, X>) this;
        }

        public Seq<I, O> $() {
            return new Seq<>(ss.freeze());
        }

    }

}