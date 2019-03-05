package mb.statix.scopegraph.reference;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.util.Capsules;

public abstract class ScopeGraph<V, L, R> implements IScopeGraph<V, L, R> {

    protected ScopeGraph() {
    }

    @Override public Set.Immutable<V> getAllScopes() {
        Set.Transient<V> allScopes = Set.Transient.of();
        allScopes.__insertAll(getEdges().keySet());
        allScopes.__insertAll(getEdges().valueSet());
        allScopes.__insertAll(getData().keySet());
        return allScopes.freeze();
    }

    // ------------------------------------

    public static class Immutable<V, L, R> extends ScopeGraph<V, L, R>
            implements IScopeGraph.Immutable<V, L, R>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<L> labels;
        private final L endOfPath;
        private final Set.Immutable<R> relations;

        private final IRelation3.Immutable<V, L, V> edges;
        private final IRelation3.Immutable<V, R, List<V>> data;

        Immutable(Set.Immutable<L> labels, L endOfPath, Set.Immutable<R> relations, IRelation3.Immutable<V, L, V> edges,
                IRelation3.Immutable<V, R, List<V>> data) {
            this.labels = labels;
            this.endOfPath = endOfPath;
            assert labels.contains(endOfPath);
            this.relations = relations;
            this.edges = edges;
            this.data = data;
        }

        @Override public Set.Immutable<L> getLabels() {
            return labels;
        }

        @Override public L getEndOfPath() {
            return endOfPath;
        }

        @Override public Set.Immutable<R> getRelations() {
            return relations;
        }

        // ------------------------------------------------------------

        @Override public IRelation3.Immutable<V, L, V> getEdges() {
            return edges;
        }

        @Override public IRelation3.Immutable<V, R, List<V>> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<V, L, R> addEdge(V sourceScope, L label, V targetScope) {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges.put(sourceScope, label, targetScope),
                    data);
        }

        @Override public ScopeGraph.Immutable<V, L, R> addDatum(V sourceScope, R relation, Iterable<V> datum) {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges,
                    data.put(sourceScope, relation, ImmutableList.copyOf(datum)));
        }

        // ------------------------------------------------------------

        public ScopeGraph.Transient<V, L, R> melt() {
            return new ScopeGraph.Transient<>(labels, endOfPath, relations, edges.melt(), data.melt());
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
            @SuppressWarnings("unchecked") ScopeGraph.Immutable<V, L, R> other = (ScopeGraph.Immutable<V, L, R>) obj;
            if(!edges.equals(other.edges))
                return false;
            if(!data.equals(other.data))
                return false;
            return true;
        }

        public static <V, L, R> ScopeGraph.Immutable<V, L, R> of(Iterable<L> labels, L endOfPath,
                Iterable<R> relations) {
            return new ScopeGraph.Immutable<>(Capsules.newSet(labels), endOfPath, Capsules.newSet(relations),
                    HashTrieRelation3.Immutable.of(), HashTrieRelation3.Immutable.of());
        }

    }

    public static class Transient<V, L, R> extends ScopeGraph<V, L, R> implements IScopeGraph.Transient<V, L, R> {

        private final Set.Immutable<L> labels;
        private final L endOfPath;
        private final Set.Immutable<R> relations;

        private final IRelation3.Transient<V, L, V> edges;
        private final IRelation3.Transient<V, R, List<V>> data;

        Transient(Set.Immutable<L> labels, L endOfPath, Set.Immutable<R> relations, IRelation3.Transient<V, L, V> edges,
                IRelation3.Transient<V, R, List<V>> data) {
            this.labels = labels;
            this.endOfPath = endOfPath;
            assert labels.contains(endOfPath);
            this.relations = relations;
            this.edges = edges;
            this.data = data;
        }

        @Override public Set.Immutable<L> getLabels() {
            return labels;
        }

        @Override public L getEndOfPath() {
            return endOfPath;
        }

        @Override public Set.Immutable<R> getRelations() {
            return relations;
        }

        // ------------------------------------------------------------

        @Override public IRelation3<V, L, V> getEdges() {
            return edges;
        }

        @Override public IRelation3<V, R, List<V>> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public boolean addEdge(V sourceScope, L label, V targetScope) {
            return edges.put(sourceScope, label, targetScope);
        }

        @Override public boolean addDatum(V scope, R relation, Iterable<V> datum) {
            return data.put(scope, relation, ImmutableList.copyOf(datum));
        }

        @Override public boolean addAll(IScopeGraph<V, L, R> other) {
            boolean change = false;
            change |= edges.putAll(other.getEdges());
            change |= data.putAll(other.getData());
            return change;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<V, L, R> freeze() {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges.freeze(), data.freeze());
        }

        public static <V, L, R> ScopeGraph.Transient<V, L, R> of(Set.Immutable<L> labels, L endOfPath,
                Set.Immutable<R> relations) {
            return new ScopeGraph.Transient<>(labels, endOfPath, relations, HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of());
        }

    }

}