package mb.p_raffrayi.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

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
        return reachableVertices(vertex, graph).size() == graph.vertices().size();
    }

    /**
     * Computes all strongly connected components in a graph.
     *
     * @param <V> Graph node type.
     * @param graph The graph in which to find strongly connected components.
     * @return A set of strongly connected components, where each component is represented by the set of vertices it includes.
     */
    public static <V> Set<Set<V>> sccs(IGraph<V> graph) {
        final SetMultimap<V, V> reachables = HashMultimap.create();
        final Set<V> vertices = new HashSet<>(graph.vertices());

        // Create a map of all transitively reachable vertices.
        for(V vertex: vertices) {
            reachables.putAll(vertex, reachableVertices(vertex, graph));
        }

        // For each vertex, see of all reachable vertices have the same reachable set.
        // If that it the case, its reachable set is a SCC.
        // Otherwise, there are edges in
        final Set<Set<V>> sccs = new HashSet<>();
        outer:
        while(!vertices.isEmpty()) {
            final V currentVertex = vertices.iterator().next();
            final Set<V> currentTargets = reachables.get(currentVertex);

            inner:
            for(V other : currentTargets) {
                if(other == currentVertex) {
                    continue inner;
                }
                final Set<V> otherTargets = reachables.get(other);
                if(!otherTargets.contains(currentVertex)) {
                    // `currentVertex` is not reachable from `other`.
                    // `otherTargets` must be smaller than `currentTargets`,
                    // as all vertices in `otherTargets` are reachable from `currentVertex`
                    // because `other` is reachable from `currentVertex`.
                    // Therefore, only `otherTargets` is still eligible as SCC.
                    vertices.removeAll(Sets.difference(currentTargets, otherTargets));
                    continue outer;
                }
            }

            // All vertices have equal target set, so knot detected
            sccs.add(currentTargets);
            vertices.removeAll(currentTargets);
        }

        return sccs;
    }

    public static <V> Set<V> reachableVertices(V vertex, IGraph<V> graph) {
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

        return reachable;
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
