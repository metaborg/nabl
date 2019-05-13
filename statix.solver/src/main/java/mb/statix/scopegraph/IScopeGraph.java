package mb.statix.scopegraph;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.IRelation3;

public interface IScopeGraph<S, L, D> {

    Set.Immutable<L> getEdgeLabels();

    L getNoDataLabel();

    Set.Immutable<L> getDataLabels();

    Set<S> getAllScopes();

    IRelation3<S, L, S> getEdges();

    IRelation3<S, L, D> getData();

    interface Immutable<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Immutable<S> getAllScopes();

        @Override IRelation3.Immutable<S, L, S> getEdges();

        @Override IRelation3<S, L, D> getData();

        Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope);

        Immutable<S, L, D> addDatum(S scope, L relation, D datum);

        Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other);

        IScopeGraph.Transient<S, L, D> melt();

    }

    interface Transient<S, L, D> extends IScopeGraph<S, L, D> {

        boolean addEdge(S sourceScope, L label, S targetScope);

        boolean addDatum(S scope, L relation, D datum);

        boolean addAll(IScopeGraph<S, L, D> other);

        // -----------------------

        IScopeGraph.Immutable<S, L, D> freeze();

    }

}