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

    @Override public abstract IRelation3<S, L, S> getEdges();

    @Override public java.util.Set<S> getEdges(S scope, L label) {
        return getEdges().get(scope, label);
    }

    @Override public abstract IRelation3<S, L, List<D>> getData();

    @Override public java.util.Set<List<D>> getData(S scope, L relation) {
        return getData().get(scope, relation);
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

        private final Set.Immutable<L> labels;
        private final L endOfPath;
        private final Set.Immutable<L> relations;

        private final IRelation3.Immutable<S, L, S> edges;
        private final IRelation3.Immutable<S, L, List<D>> data;

        Immutable(Set.Immutable<L> labels, L endOfPath, Set.Immutable<L> relations, IRelation3.Immutable<S, L, S> edges,
                IRelation3.Immutable<S, L, List<D>> data) {
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

        @Override public Set.Immutable<L> getRelations() {
            return relations;
        }

        // ------------------------------------------------------------

        @Override public IRelation3<S, L, S> getEdges() {
            return edges;
        }

        @Override public IRelation3<S, L, List<D>> getData() {
            return data;
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Immutable<S, L, D> addEdge(S sourceScope, L label, S targetScope) {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges.put(sourceScope, label, targetScope),
                    data);
        }

        @Override public ScopeGraph.Immutable<S, L, D> addDatum(S sourceScope, L relation, Iterable<D> datum) {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges,
                    data.put(sourceScope, relation, ImmutableList.copyOf(datum)));
        }

        @Override public IScopeGraph.Immutable<S, L, D> addAll(IScopeGraph<S, L, D> other) {
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges.putAll(other.getEdges()),
                    data.putAll(other.getData()));
        }

        // ------------------------------------------------------------

        @Override public ScopeGraph.Transient<S, L, D> melt() {
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
            @SuppressWarnings("unchecked") ScopeGraph.Immutable<S, L, D> other = (ScopeGraph.Immutable<S, L, D>) obj;
            if(!edges.equals(other.edges))
                return false;
            if(!data.equals(other.data))
                return false;
            return true;
        }

        public static <S extends D, L, D> ScopeGraph.Immutable<S, L, D> of(Iterable<L> labels, L endOfPath,
                Iterable<L> relations) {
            return new ScopeGraph.Immutable<>(Capsules.newSet(labels), endOfPath, Capsules.newSet(relations),
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

        private final Set.Immutable<L> labels;
        private final L endOfPath;
        private final Set.Immutable<L> relations;

        private final IRelation3.Transient<S, L, S> edges;
        private final IRelation3.Transient<S, L, List<D>> data;

        Transient(Set.Immutable<L> labels, L endOfPath, Set.Immutable<L> relations, IRelation3.Transient<S, L, S> edges,
                IRelation3.Transient<S, L, List<D>> data) {
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

        @Override public Set.Immutable<L> getRelations() {
            return relations;
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
            return new ScopeGraph.Immutable<>(labels, endOfPath, relations, edges.freeze(), data.freeze());
        }

        public static <S extends D, L, D> ScopeGraph.Transient<S, L, D> of(Set.Immutable<L> labels, L endOfPath,
                Set.Immutable<L> relations) {
            return new ScopeGraph.Transient<>(labels, endOfPath, relations, HashTrieRelation3.Transient.of(),
                    HashTrieRelation3.Transient.of());
        }

    }

}