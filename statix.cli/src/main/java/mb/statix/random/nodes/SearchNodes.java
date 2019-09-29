package mb.statix.random.nodes;

import java.util.Objects;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableList;

public class SearchNodes<O> implements SearchElement {

    private final Stream<SearchNode<O>> nodes;
    private final SearchNode<?> parent;
    private final String desc;

    private SearchNodes(Stream<SearchNode<O>> nodes, SearchNode<?> parent, String desc) {
        this.nodes = nodes;
        this.parent = parent;
        this.desc = desc;
    }

    @Override public int id() {
        return -1;
    }

    public Stream<SearchNode<O>> nodes() {
        return nodes;
    }

    @Override public SearchNode<?> parent() {
        return parent;
    }

    @Override public String desc() {
        return desc;
    }

    @Override public String toString() {
        return desc != null ? desc : Objects.toString(this);
    }

    // stream delegates

    public SearchNodes<O> limit(int n) {
        return new SearchNodes<>(nodes.limit(n), parent, desc);
    }

    public SearchNodes<O> filter(Predicate1<SearchNode<O>> filter) {
        return new SearchNodes<>(nodes.filter(filter::test), parent, desc);
    }

    public <R> SearchNodes<R> map(Function1<SearchNode<O>, SearchNode<R>> map) {
        return new SearchNodes<>(nodes.map(map::apply), parent, desc);
    }

    public <R> SearchNodes<R> flatMap(Function1<SearchNode<O>, SearchNodes<R>> flatMap) {
        return new SearchNodes<>(nodes.flatMap(n -> {
            return flatMap.apply(n).nodes;
        }), parent, desc);
    }

    // construction methods

    public static <O> SearchNodes<O> empty(SearchNode<?> parent, String desc) {
        return new SearchNodes<>(Stream.empty(), parent, desc);
    }

    @SafeVarargs public static <O> SearchNodes<O> of(SearchNode<?> parent, SearchNode<O>... nodes) {
        return new SearchNodes<>(ImmutableList.copyOf(nodes).stream(), parent, "");
    }

    public static <O> SearchNodes<O> of(SearchNode<?> parent, Stream<SearchNode<O>> nodes) {
        return new SearchNodes<>(nodes, parent, "");
    }

}