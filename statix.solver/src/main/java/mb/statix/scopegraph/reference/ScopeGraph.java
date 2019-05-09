package mb.statix.scopegraph.reference;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.util.Capsules;

public abstract class ScopeGraph<S extends D, L, D> implements IScopeGraph<S, L, D> {

    protected ScopeGraph() {
    }

    @Override public Set.Immutable<S> getAllScopes() {
        Set.Transient<S> allScopes = Set.Transient.of();
        allScopes.__insertAll(getEdges().keySet());
        allScopes.__insertAll(getEdges().valueSet());
        allScopes.__insertAll(getData().keySet());
        return allScopes.freeze();
    }

    // ------------------------------------

    public static class Immutable<S extends D, L, D> extends ScopeGraph<S, L, D>
            implements IScopeGraph.Immutable<S, L, D>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<L> edgeLabels;
        private final Set.Immutable<L> dataLabels;
        private final L noDataLabel;

        private final IRelation3.Immutable<S, L, S> edges;
        private final IRelation3.Immutable<S, L, List<D>> data;

        Immutable(Set.Immutable<L> edgeLabels, Set.Immutable<L> dataLabels, L noDataLabel,
                IRelation3.Immutable<S, L, S> edges, IRelation3.Immutable<S, L, List<D>> data) {
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

        @Override public IRelation3.Immutable<S, L, S> getEdges() {
            return edges;
        }

        @Override public IRelation3.Immutable<S, L, List<D>> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope) {
            return new ScopeGraph.Immutable<>(edgeLabels, dataLabels, noDataLabel,
                    edges.put(sourceScope, label, targetScope), data);
        }

        @Override public ScopeGraph.Immutable<S, L, D> addDatum(S sourceScope, L relation, Iterable<D> datum) {
            return new ScopeGraph.Immutable<>(edgeLabels, dataLabels, noDataLabel, edges,
                    data.put(sourceScope, relation, ImmutableList.copyOf(datum)));
        }

        @Override public IScopeGraph.Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other) {
            return new ScopeGraph.Immutable<>(edgeLabels, dataLabels, noDataLabel, edges.putAll(other.getEdges()),
                    data.putAll(other.getData()));
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Transient<S, L, D> melt() {
            return new ScopeGraph.Transient<>(edgeLabels, dataLabels, noDataLabel, edges.melt(), data.melt());
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
            return new ScopeGraph.Immutable<>(Capsules.newSet(edgeLabels), Capsules.newSet(dataLabels), noDataLabel,
                    HashTrieRelation3.Immutable.of(), HashTrieRelation3.Immutable.of());
        }

        // ------------------------------------------------------------

        @Override public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{");
            final AtomicBoolean first = new AtomicBoolean(true);
            edges.stream().forEach(edge -> {
                sb.append(first.getAndSet(false) ? " " : ", ");
                sb.append(edge._1());
                sb.append(" -");
                sb.append(edge._2());
                sb.append("-> ");
                sb.append(edge._3());
            });
            data.stream().forEach(datum -> {
                sb.append(first.getAndSet(false) ? " " : ", ");
                sb.append(datum._1());
                sb.append(" -");
                sb.append(datum._2());
                sb.append("-[ ");
                sb.append(datum._3());
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

        private final IRelation3.Transient<S, L, S> edges;
        private final IRelation3.Transient<S, L, List<D>> data;

        Transient(Set.Immutable<L> edgeLabels, Set.Immutable<L> dataLabels, L noDataLabel,
                IRelation3.Transient<S, L, S> edges, IRelation3.Transient<S, L, List<D>> data) {
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

        @Override public IRelation3<S, L, S> getEdges() {
            return edges;
        }

        @Override public IRelation3<S, L, List<D>> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public boolean addEdge(S sourceScope, L label, S targetScope) {
            return edges.put(sourceScope, label, targetScope);
        }

        @Override public boolean addDatum(S scope, L relation, Iterable<D> datum) {
            return data.put(scope, relation, ImmutableList.copyOf(datum));
        }

        @Override public boolean addAll(IScopeGraph<S, L, D> other) {
            boolean change = false;
            change |= edges.putAll(other.getEdges());
            change |= data.putAll(other.getData());
            return change;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<S, L, D> freeze() {
            return new ScopeGraph.Immutable<>(edgeLabels, dataLabels, noDataLabel, edges.freeze(), data.freeze());
        }

        public static <S extends D, L, D> ScopeGraph.Transient<S, L, D> of(Iterable<L> edgeLabels,
                Iterable<L> dataLabels, L noDataLabel) {
            return new ScopeGraph.Transient<>(Capsules.newSet(edgeLabels), Capsules.newSet(dataLabels), noDataLabel,
                    HashTrieRelation3.Transient.of(), HashTrieRelation3.Transient.of());
        }

    }

}