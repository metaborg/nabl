package mb.statix.random.nodes;

import java.util.stream.Stream;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

public class SearchNodes<O> implements SearchElement {

    private final Stream<SearchNode<O>> nodes;
    private final Function0<String> desc;
    private final SearchNode<?> parent;

    private SearchNodes(Stream<SearchNode<O>> nodes, Function0<String> desc, SearchNode<?> parent) {
        this.nodes = nodes;
        this.desc = desc;
        this.parent = parent;
    }

    public Stream<SearchNode<O>> nodes() {
        return nodes;
    }

    @Override public SearchNode<?> parent() {
        return parent;
    }

    @Override public String desc() {
        return desc.apply();
    }

    @Override public String toString() {
        return desc();
    }

    // stream delegates

    public SearchNodes<O> limit(int n) {
        return new SearchNodes<>(nodes.limit(n), desc, parent);
    }

    public <R> SearchNodes<R> map(Function1<SearchNode<O>, SearchNode<R>> map) {
        return new SearchNodes<>(nodes.map(map::apply), desc, parent);
    }

    // construction methods

    public static <O> SearchNodes<O> failure(SearchNode<?> parent, String error) {
        return new SearchNodes<>(Stream.empty(), () -> error, parent);
    }

    @SafeVarargs public static <O> SearchNodes<O> of(SearchNode<?> parent, Function0<String> error,
            SearchNode<O>... nodes) {
        return new SearchNodes<>(ImmutableList.copyOf(nodes).stream(), error, parent);
    }

    public static <O> SearchNodes<O> of(SearchNode<?> parent, Function0<String> error, Stream<SearchNode<O>> nodes) {
        return new SearchNodes<>(nodes, error, parent);
    }

}