package mb.statix.scopegraph;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.IRelation3;

/**
 * Interface to represent a scope graph.
 * 
 * <p>A scope graph consists of the following:
 * <ul>
 * <li>A set of edge labels {@link #getEdgeLabels()}</li>
 * <li>A set of data labels {@link #getDataLabels()}</li>
 * <li>A set of scopes {@link #getAllScopes()}</li>
 * <li>A set of edges (scope -label> scope) {@link #getEdges()}</li>
 * <li>A set of data lists (scope -relation> data) {@link #getData()}</li>
 * <ul>
 *
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 * @param <D>
 *      the type of data
 */
public interface IScopeGraph<S, L, D> {

    Set.Immutable<L> getEdgeLabels();

    L getNoDataLabel();

    Set.Immutable<L> getDataLabels();

    Set<S> getAllScopes();

    IRelation3<S, L, S> getEdges();

    java.util.Set<S> getEdges(S scope, L label);

    IRelation3<S, L, D> getData();

    java.util.Set<D> getData(S scope, L relation);

    /**
     * @see IScopeGraph
     */
    interface Immutable<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Immutable<S> getAllScopes();

        /**
         * Creates a copy of this immutable scope graph with the given edge added.
         * 
         * @param sourceScope
         *      the source scope of the edge
         * @param label
         *      the label of the edge
         * @param targetScope
         *      the target scope of the edge
         * 
         * @return
         *      the copy with the given edge added
         */
        Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope);

        /**
         * Creates a copy of this immutable scope graph with the given data added.
         * 
         * @param sourceScope
         *      the source scope
         * @param relation
         *      the relation
         * @param datum
         *      the data
         * 
         * @return
         *      the copy with the given relation added
         */
        Immutable<S, L, D> addDatum(S scope, L relation, D datum);

        Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other);

        IScopeGraph.Transient<S, L, D> melt();

    }

    /**
     * @see IScopeGraph
     */
    interface Transient<S, L, D> extends IScopeGraph<S, L, D> {

        /**
         * @param sourceScope
         *      the source scope
         * @param label
         *      the label
         * @param targetScope
         *      the target scope
         * 
         * @return
         *      true if this edge was added, false if it already existed
         */
        boolean addEdge(S sourceScope, L label, S targetScope);

        /**
         * @param scope
         *      the scope
         * @param relation
         *      the relation
         * @param datum
         *      the datum
         * 
         * @return
         *      true if this scope graph changed as a result of this call, false otherwise
         */
        boolean addDatum(S scope, L relation, D datum);

        /**
         * Add all scopes, edges and relations from the given scope graph to this scope graph.
         * 
         * @param other
         *      the other scope graph
         * 
         * @return
         *      true if this scope graph changed as a result of this call, false otherwise
         */
        boolean addAll(IScopeGraph<S, L, D> other);

        // -----------------------

        IScopeGraph.Immutable<S, L, D> freeze();

    }

}
