package mb.statix.random.strategy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function0;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.statix.random.SearchContext;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;

final class Seq<I, O> extends SearchStrategy<I, O> {

    private final List<SearchStrategy<?, ?>> ss;

    private Seq(List<SearchStrategy<?, ?>> ss) {
        this.ss = ss;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) @Override public SearchNodes<O> doApply(SearchContext ctx,
            SearchNode<I> node) {
        Stream<SearchNode> nodes = Stream.of(node);
        List<Function0<String>> descs = Lists.newArrayList();
        for(SearchStrategy s : ss) {
            nodes = nodes.flatMap(n -> {
                final SearchNodes<?> sn = s.apply(ctx, n);
                descs.add(sn::desc);
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

    public static class Builder<I, O> {

        private final ImmutableList.Builder<SearchStrategy<?, ?>> ss = ImmutableList.builder();

        public Builder(SearchStrategy<I, O> s) {
            ss.add(s);
        }

        @SuppressWarnings("unchecked") public <X> Builder<I, X> $(SearchStrategy<O, X> s) {
            ss.add(s);
            return (Builder<I, X>) this;
        }

        public Seq<I, O> $() {
            return new Seq<>(ss.build());
        }

    }


}