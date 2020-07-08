package mb.statix.concurrent.actors.deadlock;

import io.usethesource.capsule.Map;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;

public class Deadlock<N, S, T> {

    private final Map.Immutable<N, S> nodes;
    private final IRelation3.Immutable<N, T, N> edges;

    Deadlock(Map.Immutable<N, S> nodes, IRelation3.Immutable<N, T, N> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public Map.Immutable<N, S> nodes() {
        return nodes;
    }

    public IRelation3.Immutable<N, T, N> edges() {
        return edges;
    }

    static <N, S, T> Deadlock<N, S, T> of(N node, S state) {
        return new Deadlock<>(Map.Immutable.of(node, state), HashTrieRelation3.Immutable.of());
    }

}