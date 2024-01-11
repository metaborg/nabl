package mb.scopegraph.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SchemaEdge<K, L, M> {

    private final Map<K, Cardinality> sources;

    private final Map<K, Cardinality> targets;

    private final L label;

    private final M meta; // custom metadata attached to schema edge

    private SchemaEdge(Map<K, Cardinality> sources, Map<K, Cardinality> targets, L label, M meta) {
        if(sources.isEmpty()) {
            throw new IllegalArgumentException("Cannot have empty scheme edge sources.");
        }

        if(targets.isEmpty()) {
            throw new IllegalArgumentException("Cannot have empty scheme edge targets.");
        }

        this.sources = sources;
        this.targets = targets;
        this.label = label;
        this.meta = meta;
    }

    public Map<K, Cardinality> getSources() {
        return sources;
    }

    public Map<K, Cardinality> getTargets() {
        return targets;
    }

    public L getLabel() {
        return label;
    }

    public M getMeta() {
        return meta;
    }

    public static <K, L, M> Builder<K, L, M> builder(L label, M meta) {
        return new Builder<>(label, meta);
    }

    public static class Builder<K, L, M> {

        private final Map<K, Cardinality> sources = new HashMap<>();

        private final Map<K, Cardinality> targets = new HashMap<>();

        private final L label;

        private final M meta;

        private Builder(L label, M meta) {
            this.label = label;
            this.meta = meta;
        }

        public Builder<K, L, M> addSource(K node, Cardinality card) {
            sources.put(node, card);
            return this;
        }

        public Builder<K, L, M> addTarget(K node, Cardinality card) {
            targets.put(node, card);
            return this;
        }

        public SchemaEdge<K, L, M> build() {
            return new SchemaEdge<>(Collections.unmodifiableMap(sources), Collections.unmodifiableMap(targets), label, meta);
        }

    }


}
