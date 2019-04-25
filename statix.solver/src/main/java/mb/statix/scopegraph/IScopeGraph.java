package mb.statix.scopegraph;

import java.util.List;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.IRelation3;

public interface IScopeGraph<S, L, D> {

    Set.Immutable<L> getLabels();

    L getEndOfPath();

    Set.Immutable<L> getRelations();

    Set<S> getAllScopes();

    IRelation3<S, L, S> getEdges();

    java.util.Set<S> getEdges(S scope, L label);

    IRelation3<S, L, List<D>> getData();

    java.util.Set<List<D>> getData(S scope, L relation);

    interface Immutable<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Immutable<S> getAllScopes();

        Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope);

        Immutable<S, L, D> addDatum(S scope, L relation, Iterable<D> datum);

        Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other);

        IScopeGraph.Transient<S, L, D> melt();

    }

    interface Transient<S, L, D> extends IScopeGraph<S, L, D> {

        boolean addEdge(S sourceScope, L label, S targetScope);

        boolean addDatum(S scope, L relation, Iterable<D> datum);

        boolean addAll(IScopeGraph<S, L, D> other);

        // -----------------------

        IScopeGraph.Immutable<S, L, D> freeze();

    }

}