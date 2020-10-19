package mb.statix.concurrent.actors.deadlock;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSetMap;

public class Deadlock<N, T> {

    private final Map.Immutable<N, Clock<N>> nodes;
    private final MultiSetMap.Immutable<Tuple2<N, N>, T> edges;

    Deadlock(Map.Immutable<N, Clock<N>> nodes, MultiSetMap.Immutable<Tuple2<N, N>, T> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public Map.Immutable<N, Clock<N>> nodes() {
        return nodes;
    }

    public MultiSetMap.Immutable<Tuple2<N, N>, T> edges() {
        return edges;
    }

    /**
     * Return all tokens the given unit is waiting for.
     */
    public SetMultimap.Immutable<N, T> outgoingWaitFors(N node) {
        return edges.toMap().entrySet().stream().filter(e -> e.getKey()._1().equals(node))
                .flatMap(e -> e.getValue().elementSet().stream().map(v -> Tuple2.of(e.getKey()._2(), v)))
                .collect(CapsuleCollectors.toSetMultimap(e -> e._1(), e -> e._2()));
    }

    static <N, T> Deadlock<N, T> empty() {
        return new Deadlock<>(Map.Immutable.of(), MultiSetMap.Immutable.of());
    }

    @Override public String toString() {
        return "Deadlock[" + nodes() + ", " + edges() + "]";
    }

}