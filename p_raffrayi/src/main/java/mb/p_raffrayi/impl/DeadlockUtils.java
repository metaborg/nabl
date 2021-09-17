package mb.p_raffrayi.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

public class DeadlockUtils {

    private DeadlockUtils() {
        // uninstantiable
    }

    /**
     * Checks whether any node in {@code graph} is reachable from {@code vertex}.
     *
     * @param <V> The type of the vertices.
     * @param vertex The vertex to start querying.
     * @param graph The graph in which to check connectivity.
     * @return {@code true} if all nodes in {@code graph} can be reached, {@code false} otherwise.
     */
    public static <V> boolean connectedToAll(V vertex, IGraph<V> graph) {
        final Queue<V> queue = new LinkedList<>();
        final Set<V> reachable = new HashSet<>();

        queue.add(vertex);
        reachable.add(vertex);

        while(!queue.isEmpty()) {
            final V head = queue.remove();
            for(V target : graph.targets(head)) {
                if(!reachable.contains(target)) {
                    queue.add(target);
                    reachable.add(target);
                }
            }
        }

        return reachable.size() == graph.vertices().size();
    }


    public interface IGraph<V> {

        Collection<V> vertices();

        Collection<V> targets(V vertex);
    }

    private static class Graph<V> implements IGraph<V> {

        private final ImmutableSet<V> vertices;

        private final ImmutableMultimap<V, V> edges;

        public Graph(ImmutableSet<V> vertices, ImmutableMultimap<V, V> edges) {
            this.edges = edges;
            this.vertices = vertices;
        }

        @Override public Collection<V> vertices() {
            return vertices;
        }

        @Override public Collection<V> targets(V vertex) {
            return edges.get(vertex);
        }

    }

    public static class GraphBuilder<V> {

        private ImmutableSet.Builder<V> vertices = ImmutableSet.builder();

        private ImmutableMultimap.Builder<V, V> edges = ImmutableMultimap.builder();

        private GraphBuilder() {

        }

        public static <V> GraphBuilder<V> of() {
            return new GraphBuilder<>();
        }

        public GraphBuilder<V> addVertex(V vertex) {
            vertices.add(vertex);
            return this;
        }

        public GraphBuilder<V> addEdge(V source, V target) {
            addVertex(source);
            addVertex(target);
            edges.put(source, target);
            return this;
        }

        public IGraph<V> build() {
            return new Graph<>(vertices.build(), edges.build());
        }

    }

}
