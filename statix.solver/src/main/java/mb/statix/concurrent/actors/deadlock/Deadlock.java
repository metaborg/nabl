package mb.statix.concurrent.actors.deadlock;

import io.usethesource.capsule.Map;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSetMap;

public class Deadlock<N, S, T> {

    private final Map.Immutable<N, S> nodes;
    private final MultiSetMap.Immutable<Tuple2<N, N>, T> edges;

    Deadlock(Map.Immutable<N, S> nodes, MultiSetMap.Immutable<Tuple2<N, N>, T> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public Map.Immutable<N, S> nodes() {
        return nodes;
    }

    public MultiSetMap.Immutable<Tuple2<N, N>, T> edges() {
        return edges;
    }

    static <N, S, T> Deadlock<N, S, T> of(N node, S state) {
        return new Deadlock<>(Map.Immutable.of(node, state), MultiSetMap.Immutable.of());
    }

    @Override public String toString() {
        return "Deadlock[" + nodes() + ", " + edges() + "]";
    }

}