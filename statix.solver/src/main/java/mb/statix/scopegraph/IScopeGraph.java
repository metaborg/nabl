package mb.statix.scopegraph;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;

public interface IScopeGraph<S, L, D> {

    Set<L> getEdgeLabels();

    Map<? extends Entry<S, L>, ? extends Iterable<S>> getEdges();

    Iterable<S> getEdges(S scope, L label);

    Map<S, D> getData();

    Optional<D> getData(S scope);

    @Deprecated default Iterable<D> getData(S scope, L label) {
        return Streams.stream(getEdges(scope, label)).flatMap(s -> Streams.stream(getData(s)))
                .collect(ImmutableList.toImmutableList());
    }

    interface Immutable<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Immutable<L> getEdgeLabels();

        Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope);

        Immutable<S, L, D> setDatum(S scope, D datum);

        Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other);

        IScopeGraph.Transient<S, L, D> melt();

    }

    interface Transient<S, L, D> extends IScopeGraph<S, L, D> {

        @Override Set.Transient<L> getEdgeLabels();

        boolean addEdge(S sourceScope, L label, S targetScope);

        boolean setDatum(S scope, D datum);

        boolean addAll(IScopeGraph<S, L, D> other);

        // -----------------------

        IScopeGraph.Immutable<S, L, D> freeze();

    }

}
