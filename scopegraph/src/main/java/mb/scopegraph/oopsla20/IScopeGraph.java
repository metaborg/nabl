package mb.scopegraph.oopsla20;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import io.usethesource.capsule.Set;

public interface IScopeGraph<S, L, D> {

    Set<L> getLabels();

    Map<? extends Entry<S, L>, ? extends Iterable<S>> getEdges();

    Iterable<S> getEdges(S scope, L label);

    Map<S, D> getData();

    Optional<D> getData(S scope);

    interface Immutable<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Immutable<L> getLabels();

        Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope);

        Immutable<S, L, D> setDatum(S scope, D datum);

        Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other);

        IScopeGraph.Transient<S, L, D> melt();

    }

    interface Transient<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Transient<L> getLabels();

        boolean addEdge(S sourceScope, L label, S targetScope);

        boolean setDatum(S scope, D datum);

        boolean addAll(IScopeGraph<S, L, D> other);

        // -----------------------

        IScopeGraph.Immutable<S, L, D> freeze();

    }

}
