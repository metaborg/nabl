package mb.statix.scopegraph;

import java.util.Map;
import java.util.Map.Entry;

import io.usethesource.capsule.Set;

public interface IScopeGraph<S, L, D> {

    Set.Immutable<L> getEdgeLabels();

    L getNoDataLabel();

    Set.Immutable<L> getDataLabels();

    Map<? extends Entry<S, L>, ? extends Iterable<S>> getEdges();

    Iterable<S> getEdges(S scope, L label);

    Map<? extends Entry<S, L>, ? extends Iterable<D>> getData();

    Iterable<D> getData(S scope, L relation);

    interface Immutable<S, L, D> extends IScopeGraph<S, L, D> {

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