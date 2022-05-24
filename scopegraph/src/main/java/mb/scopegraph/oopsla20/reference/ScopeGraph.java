package mb.scopegraph.oopsla20.reference;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.collection.ConsList;
import org.metaborg.util.tuple.Tuple2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.IScopeGraph;

public abstract class ScopeGraph<S, L, D> implements IScopeGraph<S, L, D> {

    protected ScopeGraph() {
    }

    @Override public abstract Map<Tuple2<S, L>, ConsList<S>> getEdges();

    @Override public Iterable<S> getEdges(S scope, L label) {
        return getEdges().getOrDefault(Tuple2.of(scope, label), ConsList.nil());
    }

    @Override public abstract Map<S, D> getData();

    @Override public Optional<D> getData(S scope) {
        return Optional.ofNullable(getData().get(scope));
    }

    // ------------------------------------

    public static class Immutable<S, L, D> extends ScopeGraph<S, L, D>
            implements IScopeGraph.Immutable<S, L, D>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<L> labels;

        private final Map.Immutable<Tuple2<S, L>, ConsList<S>> edges;
        private final Map.Immutable<S, D> data;

        Immutable(Set.Immutable<L> labels, Map.Immutable<Tuple2<S, L>, ConsList<S>> edges, Map.Immutable<S, D> data) {
            this.labels = labels;
            this.edges = edges;
            this.data = data;
        }

        @Override public Set.Immutable<L> getLabels() {
            return labels;
        }

        // ------------------------------------------------------------

        @Override public Map<Tuple2<S, L>, ConsList<S>> getEdges() {
            return edges;
        }

        @Override public Map<S, D> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope) {
            final ScopeGraph.Transient<S, L, D> scopeGraph = melt();
            scopeGraph.addEdge(sourceScope, label, targetScope);
            return scopeGraph.freeze();
        }

        @Override public ScopeGraph.Immutable<S, L, D> setDatum(S sourceScope, D datum) {
            final ScopeGraph.Transient<S, L, D> scopeGraph = melt();
            scopeGraph.setDatum(sourceScope, datum);
            return scopeGraph.freeze();
        }

        @Override public IScopeGraph.Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other) {
            final ScopeGraph.Transient<S, L, D> scopeGraph = melt();
            scopeGraph.addAll(other);
            return scopeGraph.freeze();
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Transient<S, L, D> melt() {
            return new ScopeGraph.Transient<>(labels.asTransient(), edges.asTransient(), data.asTransient());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + edges.hashCode();
            result = prime * result + data.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") ScopeGraph.Immutable<S, L, D> other = (ScopeGraph.Immutable<S, L, D>) obj;
            if(!edges.equals(other.edges))
                return false;
            if(!data.equals(other.data))
                return false;
            return true;
        }

        public static <S, L, D> ScopeGraph.Immutable<S, L, D> of() {
            return new ScopeGraph.Immutable<>(Set.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
        }

        // ------------------------------------------------------------

    }

    public static class Transient<S, L, D> extends ScopeGraph<S, L, D> implements IScopeGraph.Transient<S, L, D> {

        private final Set.Transient<L> labels;

        private final Map.Transient<Tuple2<S, L>, ConsList<S>> edges;
        private final Map.Transient<S, D> data;

        Transient(Set.Transient<L> labels, Map.Transient<Tuple2<S, L>, ConsList<S>> edges, Map.Transient<S, D> data) {
            this.labels = labels;
            this.edges = edges;
            this.data = data;
        }

        @Override public Set.Transient<L> getLabels() {
            return labels;
        }

        // ------------------------------------------------------------

        @Override public Map<Tuple2<S, L>, ConsList<S>> getEdges() {
            return edges;
        }

        @Override public Map<S, D> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public boolean addEdge(S sourceScope, L label, S targetScope) {
            final Tuple2<S, L> key = Tuple2.of(sourceScope, label);
            final ConsList<S> scopes = edges.getOrDefault(key, ConsList.nil());
            edges.__put(key, scopes.prepend(targetScope));
            labels.__insert(label);
            return true;
        }

        @Override public boolean setDatum(S scope, D datum) {
            data.__put(scope, datum);
            return true;
        }

        @Override public boolean addAll(IScopeGraph<S, L, D> other) {
            for(Entry<? extends Entry<S, L>, ? extends Iterable<S>> entry : other.getEdges().entrySet()) {
                final Tuple2<S, L> key = Tuple2.of(entry.getKey());
                final Iterable<S> otherScopes = entry.getValue();
                final ConsList<S> scopes = edges.getOrDefault(key, ConsList.nil());
                final ConsList<S> mergedScopes = scopes.prepend(ConsList.of(otherScopes));
                edges.__put(Tuple2.of(key), mergedScopes);
            }
            for(Entry<S, D> entry : other.getData().entrySet()) {
                data.__put(entry.getKey(), entry.getValue());
            }
            for(L label : other.getLabels()) {
                labels.__insert(label);
            }
            return true;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<S, L, D> freeze() {
            return new ScopeGraph.Immutable<>(labels.freeze(), edges.freeze(), data.freeze());
        }

        public static <S, L, D> ScopeGraph.Transient<S, L, D> of() {
            return new ScopeGraph.Transient<>(Set.Transient.of(), Map.Transient.of(), Map.Transient.of());
        }

    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        final AtomicBoolean first = new AtomicBoolean(true);
        getEdges().forEach((key, targetScope) -> {
            sb.append(first.getAndSet(false) ? " " : ", ");
            sb.append(key._1());
            sb.append(" -");
            sb.append(key._2());
            sb.append("-> ");
            sb.append(targetScope);
        });
        getData().forEach((key, datum) -> {
            sb.append(first.getAndSet(false) ? " " : ", ");
            sb.append(key);
            sb.append(" : ");
            sb.append(datum);
        });
        sb.append(first.get() ? "}" : " }");
        return sb.toString();
    }

}
