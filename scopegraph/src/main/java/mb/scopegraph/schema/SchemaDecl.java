package mb.scopegraph.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SchemaDecl<K, L, M> {

    private final Map<K, Cardinality> sources;

    private final Map<Integer, Map<K, Cardinality>> values;

    private final L label;

    private final M meta;

    private SchemaDecl(Map<K, Cardinality> sources, Map<Integer, Map<K, Cardinality>> values, L label, M meta) {
        if(sources.isEmpty()) {
            throw new IllegalArgumentException("Cannot have empty scheme decl sources.");
        }

        this.sources = sources;
        this.values = values;
        this.label = label;
        this.meta = meta;
    }

    public Map<K, Cardinality> getSources() {
        return sources;
    }

    public Set<Integer> getValueIndices() {
        return values.keySet();
    }

    public Map<K, Cardinality> getValuesAt(int idx) {
        return values.getOrDefault(idx, Collections.emptyMap());
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

        private final Map<Integer, Map<K, Cardinality>> values = new HashMap<>();

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

        public Builder<K, L, M> addValue(int idx, K node, Cardinality card) {
            values.computeIfAbsent(idx, HashMap::new).put(node, card);
            return this;
        }

        public SchemaDecl<K, L, M> build() {
            return new SchemaDecl<>(Collections.unmodifiableMap(sources), Collections.unmodifiableMap(values), label, meta);
        }

    }

}
