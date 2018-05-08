package mb.statix.scopegraph.reference;

import java.io.Serializable;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.IScopeGraph;

public abstract class ScopeGraph<S, L, R, O> implements IScopeGraph<S, L, R, O> {

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

    public static class Immutable<S, L, R, O> extends ScopeGraph<S, L, R, O>
            implements IScopeGraph.Immutable<S, L, R, O>, Serializable {
        private static final long serialVersionUID = 42L;

        private final Set.Immutable<L> labels;
        private final L endOfPath;
        private final Set.Immutable<R> relations;

        private final IRelation3.Immutable<S, L, S> edges;
        private final IRelation3.Immutable<S, R, O> data;

        Immutable(Set.Immutable<L> labels, L endOfPath, Set.Immutable<R> relations, IRelation3.Immutable<S, L, S> edges,
                IRelation3.Immutable<S, R, O> data) {
            this.labels = labels;
            this.endOfPath = endOfPath;
            assert labels.contains(endOfPath);
            this.relations = relations;
            this.edges = edges;
            this.data = data;
        }

        public Set.Immutable<L> getLabels() {
            return labels;
        }

        public L getEndOfPath() {
            return endOfPath;
        }

        public Set.Immutable<R> getRelations() {
            return relations;
        }

        // ------------------------------------------------------------

        @Override public IRelation3.Immutable<S, L, S> getEdges() {
            return edges;
        }

        @Override public IRelation3.Immutable<S, R, O> getData() {
            return data;
        }

        // ------------------------------------------------------------

        public ScopeGraph.Transient<S, L, R, O> melt() {
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
            @SuppressWarnings("unchecked") ScopeGraph.Immutable<S, L, R, O> other =
                    (ScopeGraph.Immutable<S, L, R, O>) obj;
            if(!edges.equals(other.edges))
                return false;
            if(!data.equals(other.data))
                return false;
            return true;
        }

        public static <S, L, R, O> ScopeGraph.Immutable<S, L, R, O> of(Set.Immutable<L> labels, L endOfPath,
                Set.Immutable<R> relations) {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, HashTrieRelation3.Immutable.of(),
                    HashTrieRelation3.Immutable.of());
        }

    }

    public static class Transient<S, L, R, O> extends ScopeGraph<S, L, R, O>
            implements IScopeGraph.Transient<S, L, R, O> {

        private final Set.Immutable<L> labels;
        private final L endOfPath;
        private final Set.Immutable<R> relations;

        private final IRelation3.Transient<S, L, S> edges;
        private final IRelation3.Transient<S, R, O> data;

        Transient(Set.Immutable<L> labels, L endOfPath, Set.Immutable<R> relations, IRelation3.Transient<S, L, S> edges,
                IRelation3.Transient<S, R, O> data) {
            this.labels = labels;
            this.endOfPath = endOfPath;
            assert labels.contains(endOfPath);
            this.relations = relations;
            this.edges = edges;
            this.data = data;
        }

        public Set.Immutable<L> getLabels() {
            return labels;
        }

        public L getEndOfPath() {
            return endOfPath;
        }

        public Set.Immutable<R> getRelations() {
            return relations;
        }

        // ------------------------------------------------------------

        @Override public IRelation3<S, L, S> getEdges() {
            return edges;
        }

        @Override public IRelation3<S, R, O> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public boolean addEdge(S sourceScope, L label, S targetScope) {
            return edges.put(sourceScope, label, targetScope);
        }

        @Override public boolean addDatum(S scope, R relation, O decl) {
            return data.put(scope, relation, decl);
        }

        @Override public boolean addAll(IScopeGraph<S, L, R, O> other) {
            boolean change = false;
            change |= edges.putAll(other.getEdges());
            change |= data.putAll(other.getData());
            return change;
        }

        // ------------------------------------------------------------

        public ScopeGraph.Immutable<S, L, R, O> freeze() {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges.freeze(), data.freeze());
        }

        public static <S, L, R, O> ScopeGraph.Transient<S, L, R, O> of(Set.Immutable<L> labels, L endOfPath,
                Set.Immutable<R> relations) {
            return new ScopeGraph.Transient<>(labels, endOfPath, relations, HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of());
        }

    }

}