package mb.statix.concurrent.actors.deadlock;

import io.usethesource.capsule.Map;

public class Deadlock<N> {

    private final Map.Immutable<N, Clock<N>> nodes;

    Deadlock(Map.Immutable<N, Clock<N>> nodes) {
        this.nodes = nodes;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public Map.Immutable<N, Clock<N>> nodes() {
        return nodes;
    }

    static <N, T> Deadlock<N> empty() {
        return new Deadlock<>(Map.Immutable.of());
    }

    @Override public String toString() {
        return "Deadlock{" + nodes() + "}";
    }

}