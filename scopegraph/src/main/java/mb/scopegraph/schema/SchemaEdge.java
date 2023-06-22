package mb.scopegraph.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SchemaEdge<K, L> {

    private final Map<K, Cardinality> sources;

    private final Map<K, Cardinality> targets;

    private final L label;

    private SchemaEdge(Map<K, Cardinality> sources, Map<K, Cardinality> targets, L label) {
        if(sources.isEmpty()) {
            throw new IllegalArgumentException("Cannot have empty scheme edge sources.");
        }

        if(targets.isEmpty()) {
            throw new IllegalArgumentException("Cannot have empty scheme edge targets.");
        }

        this.sources = sources;
        this.targets = targets;
        this.label = label;
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

    public static <K, L> Builder<K, L> builder(L label) {
        return new Builder<>(label);
    }

    public static class Builder<K, L> {

        private final Map<K, Cardinality> sources = new HashMap<>();

        private final Map<K, Cardinality> targets = new HashMap<>();

        private final L label;

        private Builder(L label) {
            this.label = label;
        }

        public Builder<K, L> addSource(K node, Cardinality card) {
            sources.put(node, card);
            return this;
        }

        public Builder<K, L> addTarget(K node, Cardinality card) {
            targets.put(node, card);
            return this;
        }

        public SchemaEdge<K, L> build() {
            return new SchemaEdge<>(Collections.unmodifiableMap(sources), Collections.unmodifiableMap(targets), label);
        }

    }


}
