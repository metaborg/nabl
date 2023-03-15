package mb.p_raffrayi.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.collection.Sets;

import io.usethesource.capsule.BinaryRelation;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.Set.Transient;
import io.usethesource.capsule.SetMultimap;

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
     * Checks whether any node in {@code graph} can reach {@code vertex}.
     *
     * @param <V> The type of the vertices.
     * @param vertex The vertex that is to be reachable.
     * @param graph The graph in which to check connectivity.
     * @return {@code true} if all nodes in {@code graph} can {@code vertex}, {@code false} otherwise.
     */
    public static <V> boolean allConnectedTo(V vertex, IGraph<V> graph) {
        return reachableVertices(vertex, graph.invert()).size() == graph.vertices().size();
    }

    /**
     * Computes all strongly connected components in a graph.
     *
     * @param <V> Graph node type.
     * @param graph The graph in which to find strongly connected components.
     * @return A set of strongly connected components, where each component is represented by the set of vertices it includes.
     */
    public static <V> Set<Set<V>> sccs(IGraph<V> graph) {
        final SetMultimap.Transient<V, V> reachables = SetMultimap.Transient.of();
        final Set<V> vertices = new HashSet<>(graph.vertices());

        // Create a map of all transitively reachable vertices.
        for(V vertex: vertices) {
            reachables.__insert(vertex, reachableVertices(vertex, graph));
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

    public static <V> io.usethesource.capsule.Set.Immutable<V> reachableVertices(V vertex, IGraph<V> graph) {
        final Queue<V> queue = new LinkedList<>();
        final io.usethesource.capsule.Set.Transient<V> reachable = CapsuleUtil.transientSet();

        queue.add(vertex);
        reachable.__insert(vertex);

        while(!queue.isEmpty()) {
            final V head = queue.remove();
            for(V target : graph.targets(head)) {
                if(!reachable.contains(target)) {
                    queue.add(target);
                    reachable.__insert(target);
                }
            }
        }

        return reachable.freeze();
    }


    public interface IGraph<V> {

        Collection<V> vertices();

        Collection<V> targets(V vertex);

        IGraph<V> invert();
    }

    private static class Graph<V> implements IGraph<V> {

        private final Immutable<V> vertices;

        private final MultiSetMap.Immutable<V, V> edges;

        public Graph(Immutable<V> vertices, MultiSetMap.Immutable<V, V> edges) {
            this.edges = edges;
            this.vertices = vertices;
        }

        @Override public Collection<V> vertices() {
            return vertices;
        }

        @Override public Collection<V> targets(V vertex) {
            return edges.get(vertex).toCollection();
        }

        @Override public IGraph<V> invert() {
            final GraphBuilder<V> builder = GraphBuilder.of();
            vertices.forEach(builder::addVertex);
            edges.forEach((from, to) -> builder.addEdge(to, from));
            return builder.build();
        }

        @Override public String toString() {
            final StringBuilder sb = new StringBuilder("Graph{");
            for(V vertex : vertices) {
                sb.append("  ").append(vertex).append(":\n");
                for(V tgt: edges.get(vertex)) {
                    sb.append("    ").append(tgt).append("\n");
                }
            }
            return sb.append("}").toString();
        }

    }

    public static class GraphBuilder<V> {

        private Transient<V> vertices = CapsuleUtil.transientSet();

        private MultiSetMap.Transient<V, V> edges = MultiSetMap.Transient.of();

        private GraphBuilder() {

        }

        public static <V> GraphBuilder<V> of() {
            return new GraphBuilder<>();
        }

        public GraphBuilder<V> addVertex(V vertex) {
            vertices.__insert(vertex);
            return this;
        }

        public GraphBuilder<V> addEdge(V source, V target) {
            addVertex(source);
            addVertex(target);
            edges.put(source, target);
            return this;
        }

        public IGraph<V> build() {
            return new Graph<>(vertices.freeze(), edges.freeze());
        }

    }

}
