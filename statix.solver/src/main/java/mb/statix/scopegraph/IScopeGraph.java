package mb.statix.scopegraph;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.IRelation3;

public interface IScopeGraph<V, L, R> {

    Set.Immutable<L> getLabels();

    L getEndOfPath();

    Set.Immutable<R> getRelations();

    Set<V> getAllScopes();

    IRelation3<V, L, V> getEdges();

    IRelation3<V, R, V> getData();

    interface Immutable<V, L, R> extends IScopeGraph<V, L, R> {

        @Override Set.Immutable<V> getAllScopes();

        @Override IRelation3.Immutable<V, L, V> getEdges();

        @Override IRelation3.Immutable<V, R, V> getData();

        Immutable<V, L, R> addEdge(V sourceScope, L label, V targetScope);

        Immutable<V, L, R> addDatum(V scope, R relation, V datum);

    }

    interface Transient<V, L, R> extends IScopeGraph<V, L, R> {

        boolean addEdge(V sourceScope, L label, V targetScope);

        boolean addDatum(V scope, R relation, V datum);

        boolean addAll(IScopeGraph<V, L, R> other);

        // -----------------------

        IScopeGraph.Immutable<V, L, R> freeze();

    }

}