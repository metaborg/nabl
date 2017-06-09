package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple3;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple3;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public abstract class Properties<I, K, V> implements IProperties<I, K, V> {

    private final SetMultimap<I, K> keys;
    private final Map<Tuple2<I, K>, V> values;

    private Properties(SetMultimap<I, K> keys, Map<Tuple2<I, K>, V> values) {
        this.keys = keys;
        this.values = values;
    }

    @Override public java.util.Set<I> getIndices() {
        return keys.keySet();
    }

    @Override public Set<K> getDefinedKeys(I index) {
        return keys.get(index);
    }

    @Override public Optional<V> getValue(I index, K key) {
        return Optional.ofNullable(values.get(ImmutableTuple2.of(index, key)));
    }

    @Override public Stream<Tuple3<I, K, V>> stream() {
        return values.entrySet().stream()
                .map(entry -> ImmutableTuple3.of(entry.getKey()._1(), entry.getKey()._2(), entry.getValue()));
    }

    public static class Immutable<I, K, V> extends Properties<I, K, V>
            implements IProperties.Immutable<I, K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<I, K> keys;
        private final Map.Immutable<Tuple2<I, K>, V> values;

        private Immutable(SetMultimap.Immutable<I, K> keys, Map.Immutable<Tuple2<I, K>, V> values) {
            super(keys, values);
            this.keys = keys;
            this.values = values;
        }

        public static <I, K, V> Properties.Immutable<I, K, V> of() {
            return new Properties.Immutable<>(SetMultimap.Immutable.of(), Map.Immutable.of());
        }

        @Override public Properties.Transient<I, K, V> melt() {
            return new Properties.Transient<>(keys.asTransient(), values.asTransient());
        }

    }

    public static class Transient<I, K, V> extends Properties<I, K, V> implements IProperties.Transient<I, K, V> {

        private final SetMultimap.Transient<I, K> keys;
        private final Map.Transient<Tuple2<I, K>, V> values;

        private Transient(SetMultimap.Transient<I, K> keys, Map.Transient<Tuple2<I, K>, V> values) {
            super(keys, values);
            this.keys = keys;
            this.values = values;
        }

        @Override public Optional<V> putValue(I index, K key, V value) {
            V prev = values.__put(ImmutableTuple2.of(index, key), value);
            if(!value.equals(prev)) {
                keys.__put(index, key);
                return Optional.ofNullable(prev);
            }
            return Optional.empty();
        }

        @Override public Properties.Immutable<I, K, V> freeze() {
            return new Properties.Immutable<>(keys.freeze(), values.freeze());
        }

        public static <I, K, V> Properties.Transient<I, K, V> of() {
            return new Properties.Transient<>(SetMultimap.Transient.of(), Map.Transient.of());
        }

    }

    @Override public String toString() {
        return values.toString();
    }

    public static <I, K, V> IProperties.Transient<I, K, V> map(IProperties<I, K, V> properties,
            Function1<V, V> mapper) {
        IProperties.Transient<I, K, V> mappedProperties = Properties.Transient.of();
        properties.stream().forEach(ikv -> mappedProperties.putValue(ikv._1(), ikv._2(), mapper.apply(ikv._3())));
        return mappedProperties;
    }

}