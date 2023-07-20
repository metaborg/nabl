package mb.scopegraph.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.metaborg.util.tuple.Tuple2;

public class Schema<K /* Scope kind */, L /* Label */, M /* Meta */> {

    private final Map<K, Set<SchemaEdge<K, L, M>>> outgoingEdges;

    private final Map<K, Set<SchemaEdge<K, L, M>>> incomingEdges;

    private final Map<K, Set<SchemaDecl<K, L, M>>> decls;

    private final Map<K, Set<Tuple2<SchemaDecl<K, L, M>, Integer>>> declPositions;

    private Schema(Map<K, Set<SchemaEdge<K, L, M>>> incomingEdges, Map<K, Set<SchemaEdge<K, L, M>>> outgoingEdges,
            Map<K, Set<SchemaDecl<K, L, M>>> decls, Map<K, Set<Tuple2<SchemaDecl<K, L, M>, Integer>>> declPositions) {
        this.incomingEdges = incomingEdges;
        this.outgoingEdges = outgoingEdges;
        this.decls = decls;
        this.declPositions = declPositions;
    }

    public Set<SchemaEdge<K, L, M>> outgoingEdges(K kind) {
        return Collections.unmodifiableSet(outgoingEdges.getOrDefault(kind, Collections.emptySet()));
    }

    public Set<SchemaEdge<K, L, M>> incomingEdges(K kind) {
        return Collections.unmodifiableSet(incomingEdges.getOrDefault(kind, Collections.emptySet()));
    }

    public Set<SchemaDecl<K, L, M>> decls(K kind) {
        return Collections.unmodifiableSet(decls.getOrDefault(kind, Collections.emptySet()));
    }

    public Set<Tuple2<SchemaDecl<K, L, M>, Integer>> declPositions(K kind) {
        return Collections.unmodifiableSet(declPositions.getOrDefault(kind, Collections.emptySet()));
    }

    public static <K, L, M> Builder<K, L, M> newBuilder() {
        return new Builder<>();
    }

    public static class Builder<K, L, M> {

        private final Map<K, Set<SchemaEdge<K, L, M>>> outgoingEdges = new HashMap<>();

        private final Map<K, Set<SchemaEdge<K, L, M>>> incomingEdges = new HashMap<>();

        private final Map<K, Set<SchemaDecl<K, L, M>>> decls = new HashMap<>();

        private final Map<K, Set<Tuple2<SchemaDecl<K, L, M>, Integer>>> declPositions = new HashMap<>();

        private Builder() {
        }

        public Builder<K, L, M> addEdge(SchemaEdge<K, L, M> edge) {
            edge.getSources().keySet().forEach(k -> outgoingEdges.computeIfAbsent(k, __ -> new HashSet<>()).add(edge));
            edge.getTargets().keySet().forEach(k -> incomingEdges.computeIfAbsent(k, __ -> new HashSet<>()).add(edge));
            return this;
        }

        public Builder<K, L, M> addDecl(SchemaDecl<K, L, M> decl) {
            decl.getSources().keySet().forEach(k -> decls.computeIfAbsent(k, __ -> new HashSet<>()).add(decl));
            decl.getValueIndices().forEach(idx -> {
                decl.getValuesAt(idx).forEach((k, c) -> {
                    declPositions.computeIfAbsent(k, __ -> new HashSet<>()).add(Tuple2.of(decl, idx));
                });
            });
            return this;
        }

        public Schema<K, L, M> build() {
            return new Schema<>(Collections.unmodifiableMap(incomingEdges), Collections.unmodifiableMap(outgoingEdges),
                    Collections.unmodifiableMap(decls), Collections.unmodifiableMap(declPositions));
        }

    }

}
