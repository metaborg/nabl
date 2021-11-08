package mb.scopegraph.oopsla20;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import io.usethesource.capsule.Set;

/**
 * A scope graph.
 *
 * @param <S> the type of scopes
 * @param <L> the type of labels
 * @param <D> the type of data
 */
public interface IScopeGraph<S, L, D> {

    /**
     * Gets the set of all labels in the scope graph.
     *
     * @return the set of all labels in the scope graph
     */
    Set<L> getLabels();

    /**
     * Gets the map of all edges in the scope graph.
     *
     * @return the map of pairs of scopes and labels and the scopes each scope-label pair point to
     */
    Map<? extends Entry<S, L>, ? extends Iterable<S>> getEdges();

    /**
     * Gets all scopes from the given scope that have the specified label.
     *
     * @param scope the scope
     * @param label the label
     * @return the iterable of edges with the specified label starting at the specified scope
     */
    Iterable<S> getEdges(S scope, L label);

    /**
     * Gets the map of all data in the scope graph.
     *
     * @return a map associating scopes with their data
     */
    Map<S, D> getData();

    /**
     * Gets the data for the specified scope.
     *
     * @param scope the scope
     * @return the data, or nothing if none found
     */
    Optional<D> getData(S scope);

    /**
     * An immutable scope graph.
     *
     * @param <S> the type of scopes
     * @param <L> the type of labels
     * @param <D> the type of data
     */
    interface Immutable<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Immutable<L> getLabels();

        /**
         * Adds an edge to the scope graph.
         *
         * @param sourceScope the source scope
         * @param label the label of the edge
         * @param targetScope the target scope
         * @return the new scope graph
         */
        Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope);

        /**
         * Sets the data for the specified scope.
         *
         * @param scope the scope
         * @param datum the new data
         * @return the new scope graph
         */
        Immutable<S, L, D> setDatum(S scope, D datum);

        /**
         * Adds all labels, edges, and data from the specified scope graph to this scope graph.
         *
         * @param other the scope graph to add
         * @return the new scope graph
         */
        Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other);

        /**
         * Gets a transient copy of this scope graph.
         *
         * @return the transient copy
         */
        IScopeGraph.Transient<S, L, D> melt();

    }

    /**
     * A transient scope graph.
     *
     * @param <S> the type of scopes
     * @param <L> the type of labels
     * @param <D> the type of data
     */
    interface Transient<S, L, D> extends IScopeGraph<S, L, D> {

        /**
         * Adds an edge to the scope graph.
         *
         * @param sourceScope the source scope
         * @param label the label of the edge
         * @param targetScope the target scope
         */
        boolean addEdge(S sourceScope, L label, S targetScope);

        /**
         * Sets the data for the specified scope.
         *
         * @param scope the scope
         * @param datum the new data
         */
        boolean setDatum(S scope, D datum);

        /**
         * Adds all labels, edges, and data from the specified scope graph to this scope graph.
         *
         * @param other the scope graph to add
         */
        boolean addAll(IScopeGraph<S, L, D> other);

        /**
         * Gets an immutable copy of this scope graph.
         *
         * @return the immutable copy
         */
        IScopeGraph.Immutable<S, L, D> freeze();

    }

}
