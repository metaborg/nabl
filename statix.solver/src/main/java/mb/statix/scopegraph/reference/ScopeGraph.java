package mb.statix.scopegraph.reference;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.ConsList;
import mb.statix.scopegraph.IScopeGraph;

public abstract class ScopeGraph<S extends D, L, D> implements IScopeGraph<S, L, D> {

    protected ScopeGraph() {
    }

    @Override public abstract Map<Tuple2<S, L>, ConsList<S>> getEdges();

    @Override public Iterable<S> getEdges(S scope, L label) {
        return getEdges().getOrDefault(Tuple2.of(scope, label), ConsList.nil());
    }

    @Override public abstract Map<Tuple2<S, L>, ConsList<D>> getData();

    @Override public Iterable<D> getData(S scope, L relation) {
        return getData().getOrDefault(Tuple2.of(scope, relation), ConsList.nil());
    }

    // ------------------------------------

    public static class Immutable<S extends D, L, D> extends ScopeGraph<S, L, D>
            implements IScopeGraph.Immutable<S, L, D>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<L> edgeLabels;
        private final Set.Immutable<L> dataLabels;
        private final L noDataLabel;

        private final Map.Immutable<Tuple2<S, L>, ConsList<S>> edges;
        private final Map.Immutable<Tuple2<S, L>, ConsList<D>> data;

        Immutable(Set.Immutable<L> edgeLabels, Set.Immutable<L> dataLabels, L noDataLabel,
                Map.Immutable<Tuple2<S, L>, ConsList<S>> edges, Map.Immutable<Tuple2<S, L>, ConsList<D>> data) {
            this.edgeLabels = edgeLabels;
            this.dataLabels = dataLabels;
            this.noDataLabel = noDataLabel;
            this.edges = edges;
            this.data = data;
        }

        @Override public Set.Immutable<L> getEdgeLabels() {
            return edgeLabels;
        }

        @Override public Set.Immutable<L> getDataLabels() {
            return dataLabels;
        }

        @Override public L getNoDataLabel() {
            return noDataLabel;
        }

        // ------------------------------------------------------------

        @Override public Map<Tuple2<S, L>, ConsList<S>> getEdges() {
            return edges;
        }

        @Override public Map<Tuple2<S, L>, ConsList<D>> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope) {
            final ScopeGraph.Transient<S, L, D> scopeGraph = melt();
            scopeGraph.addEdge(sourceScope, label, targetScope);
            return scopeGraph.freeze();
        }

        @Override public ScopeGraph.Immutable<S, L, D> addDatum(S sourceScope, L relation, D datum) {
            final ScopeGraph.Transient<S, L, D> scopeGraph = melt();
            scopeGraph.addDatum(sourceScope, relation, datum);
            return scopeGraph.freeze();
        }

        @Override public IScopeGraph.Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other) {
            final ScopeGraph.Transient<S, L, D> scopeGraph = melt();
            scopeGraph.addAll(other);
            return scopeGraph.freeze();
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Transient<S, L, D> melt() {
            return new ScopeGraph.Transient<>(edgeLabels, dataLabels, noDataLabel, edges.asTransient(),
                    data.asTransient());
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

        public static <S extends D, L, D> ScopeGraph.Immutable<S, L, D> of(Iterable<L> edgeLabels,
                Iterable<L> dataLabels, L noDataLabel) {
            return new ScopeGraph.Immutable<>(CapsuleUtil.toSet(edgeLabels), CapsuleUtil.toSet(dataLabels), noDataLabel,
                    Map.Immutable.of(), Map.Immutable.of());
        }

        // ------------------------------------------------------------

        @Override public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{");
            final AtomicBoolean first = new AtomicBoolean(true);
            edges.forEach((key, targetScope) -> {
                sb.append(first.getAndSet(false) ? " " : ", ");
                sb.append(key._1());
                sb.append(" -");
                sb.append(key._2());
                sb.append("-> ");
                sb.append(targetScope);
            });
            data.forEach((key, datum) -> {
                sb.append(first.getAndSet(false) ? " " : ", ");
                sb.append(key._1());
                sb.append(" -");
                sb.append(key._2());
                sb.append("-[ ");
                sb.append(datum);
                sb.append("] ");
            });
            sb.append(first.get() ? "}" : " }");
            return sb.toString();
        }

    }

    public static class Transient<S extends D, L, D> extends ScopeGraph<S, L, D>
            implements IScopeGraph.Transient<S, L, D> {

        private final Set.Immutable<L> edgeLabels;
        private final Set.Immutable<L> dataLabels;
        private final L noDataLabel;

        private final Map.Transient<Tuple2<S, L>, ConsList<S>> edges;
        private final Map.Transient<Tuple2<S, L>, ConsList<D>> data;

        Transient(Set.Immutable<L> edgeLabels, Set.Immutable<L> dataLabels, L noDataLabel,
                Map.Transient<Tuple2<S, L>, ConsList<S>> edges, Map.Transient<Tuple2<S, L>, ConsList<D>> data) {
            this.edgeLabels = edgeLabels;
            this.dataLabels = dataLabels;
            this.noDataLabel = noDataLabel;
            this.edges = edges;
            this.data = data;
        }

        @Override public Set.Immutable<L> getEdgeLabels() {
            return edgeLabels;
        }

        @Override public Set.Immutable<L> getDataLabels() {
            return dataLabels;
        }

        @Override public L getNoDataLabel() {
            return noDataLabel;
        }

        // ------------------------------------------------------------

        @Override public Map<Tuple2<S, L>, ConsList<S>> getEdges() {
            return edges;
        }

        @Override public Map<Tuple2<S, L>, ConsList<D>> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public boolean addEdge(S sourceScope, L label, S targetScope) {
            final Tuple2<S, L> key = Tuple2.of(sourceScope, label);
            final ConsList<S> scopes = edges.getOrDefault(key, ConsList.nil());
            edges.__put(key, scopes.prepend(targetScope));
            return true;
        }

        @Override public boolean addDatum(S scope, L relation, D datum) {
            final Tuple2<S, L> key = Tuple2.of(scope, relation);
            final ConsList<D> datums = data.getOrDefault(key, ConsList.nil());
            data.__put(key, datums.prepend(datum));
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
            for(Entry<? extends Entry<S, L>, ? extends Iterable<D>> entry : other.getData().entrySet()) {
                final Tuple2<S, L> key = Tuple2.of(entry.getKey());
                final Iterable<D> otherDatums = entry.getValue();
                final ConsList<D> datums = data.getOrDefault(key, ConsList.nil());
                final ConsList<D> mergedDatums = datums.prepend(ConsList.of(otherDatums));
                data.__put(Tuple2.of(key), mergedDatums);
            }
            return true;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<S, L, D> freeze() {
            return new ScopeGraph.Immutable<>(edgeLabels, dataLabels, noDataLabel, edges.freeze(), data.freeze());
        }

        public static <S extends D, L, D> ScopeGraph.Transient<S, L, D> of(Iterable<L> edgeLabels,
                Iterable<L> dataLabels, L noDataLabel) {
            return new ScopeGraph.Transient<>(CapsuleUtil.toSet(edgeLabels), CapsuleUtil.toSet(dataLabels), noDataLabel,
                    Map.Transient.of(), Map.Transient.of());
        }

    }

}
