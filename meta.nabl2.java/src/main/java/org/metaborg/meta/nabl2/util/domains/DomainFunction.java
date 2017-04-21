package org.metaborg.meta.nabl2.util.domains;

import com.google.common.collect.SetMultimap;

public class DomainFunction<K, V> {

    private final SetMultimap<K, V> entries;
    private final boolean complement;

    private DomainFunction(SetMultimap<K, V> entries, boolean complement) {
        this.entries = entries;
        this.complement = complement;
    }

    public DomainFunction<K, V> complement() {
        return new DomainFunction<>(entries, !complement);
    }

    public Domain<V> get(K key) {
        Domain<V> value = Domain.of(entries.get(key));
        return !complement ? value : value.complement();
    }

    public boolean isEmpty() {
        return !complement && entries.isEmpty();
    }

}