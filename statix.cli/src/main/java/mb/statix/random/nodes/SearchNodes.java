package mb.statix.random.nodes;

import java.util.Objects;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

public class SearchNodes<O> implements SearchElement {

    private final Stream<SearchNode<O>> nodes;
    private final String error;
    private final SearchNode<?> parent;

    private SearchNodes(Stream<SearchNode<O>> nodes, SearchNode<?> parent) {
        this.nodes = nodes;
        this.error = null;
        this.parent = parent;
    }

    private SearchNodes(String desc, SearchNode<?> parent) {
        this.nodes = null;
        this.error = desc;
        this.parent = parent;
    }

    public boolean success() {
        return nodes != null;
    }

    public Stream<SearchNode<O>> nodes() {
        if(!success()) {
            throw new IllegalArgumentException("Cannot call nodes() on failure nodes.");
        }
        return nodes;
    }

    @Override public SearchNode<?> parent() {
        return parent;
    }

    public String error() {
        if(success()) {
            throw new IllegalArgumentException("Cannot call error() on success nodes.");
        }
        return error;
    }

    @Override public String toString() {
        return error != null ? error : Objects.toString(this);
    }

    // stream delegates

    public SearchNodes<O> limit(int n) {
        if(!success()) {
            return new SearchNodes<>(error, parent);
        }
        return new SearchNodes<>(nodes.limit(n), parent);
    }

    public <R> SearchNodes<R> map(Function1<SearchNode<O>, SearchNode<R>> map) {
        if(!success()) {
            return new SearchNodes<>(error, parent);
        }
        return new SearchNodes<>(nodes.map(map::apply), parent);
    }

    // construction methods

    public static <O> SearchNodes<O> failure(SearchNode<?> parent, String error) {
        return new SearchNodes<>(error, parent);
    }

    @SafeVarargs public static <O> SearchNodes<O> of(SearchNode<?> parent, SearchNode<O>... nodes) {
        return new SearchNodes<>(ImmutableList.copyOf(nodes).stream(), parent);
    }

    public static <O> SearchNodes<O> of(SearchNode<?> parent, Stream<SearchNode<O>> nodes) {
        return new SearchNodes<>(nodes, parent);
    }

}