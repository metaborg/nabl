package org.metaborg.meta.nabl2.util.collections;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.util.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.ImmutableTuple3;
import org.metaborg.meta.nabl2.util.Tuple2;
import org.metaborg.meta.nabl2.util.Tuple3;
import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public abstract class Properties<I, K, V> implements IProperties<I, K, V> {

    protected abstract SetMultimap<I, K> keys();

    protected abstract Map<Tuple2<I, K>, V> values();

    @Override public java.util.Set<I> getIndices() {
        return keys().keySet();
    }

    @Override public Set<K> getDefinedKeys(I index) {
        return keys().get(index);
    }

    @Override public Optional<V> getValue(I index, K key) {
        return Optional.ofNullable(values().get(ImmutableTuple2.of(index, key)));
    }

    @Override public boolean contains(I index) {
        return keys().containsKey(index);
    }

    @Override public boolean contains(I index, K key) {
        return values().containsKey(ImmutableTuple2.of(index, key));
    }

    @Override public Stream<Tuple3<I, K, V>> stream() {
        return values().entrySet().stream()
                .map(entry -> ImmutableTuple3.of(entry.getKey()._1(), entry.getKey()._2(), entry.getValue()));
    }

    @Override public String toString() {
        return values().toString();
    }


    public static class Immutable<I, K, V> extends Properties<I, K, V>
            implements IProperties.Immutable<I, K, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<I, K> keys;
        private final Map.Immutable<Tuple2<I, K>, V> values;

        private Immutable(SetMultimap.Immutable<I, K> keys, Map.Immutable<Tuple2<I, K>, V> values) {
            this.keys = keys;
            this.values = values;
        }

        @Override protected SetMultimap<I, K> keys() {
            return keys;
        }

        @Override protected Map<Tuple2<I, K>, V> values() {
            return values;
        }

        public static <I, K, V> Properties.Immutable<I, K, V> of() {
            return new Properties.Immutable<>(SetMultimap.Immutable.of(), Map.Immutable.of());
        }

        @Override public Properties.Transient<I, K, V> melt() {
            return new Properties.Transient<>(keys.asTransient(), values.asTransient());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + keys.hashCode();
            result = prime * result + values.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") final Properties.Immutable<I, K, V> other =
                    (Properties.Immutable<I, K, V>) obj;
            if(!keys.equals(other.keys))
                return false;
            if(!values.equals(other.values))
                return false;
            return true;
        }

    }


    public static class Transient<I, K, V> extends Properties<I, K, V> implements IProperties.Transient<I, K, V> {

        private final SetMultimap.Transient<I, K> keys;
        private final Map.Transient<Tuple2<I, K>, V> values;

        private Transient(SetMultimap.Transient<I, K> keys, Map.Transient<Tuple2<I, K>, V> values) {
            this.keys = keys;
            this.values = values;
        }

        @Override protected SetMultimap<I, K> keys() {
            return keys;
        }

        @Override protected Map<Tuple2<I, K>, V> values() {
            return values;
        }

        @Override public Optional<V> putValue(I index, K key, V value) {
            V prev = values.__put(ImmutableTuple2.of(index, key), value);
            if(!value.equals(prev)) {
                keys.__put(index, key);
                return Optional.ofNullable(prev);
            }
            return Optional.empty();
        }

        @Override public boolean mapValues(Function1<V, V> mapper) {
            boolean change = false;
            for(Map.Entry<Tuple2<I, K>, V> entry : values.entrySet()) {
                final V curr = entry.getValue();
                final V next = mapper.apply(curr);
                if(!next.equals(curr)) {
                    values.__put(entry.getKey(), next);
                    change |= true;
                }
            }
            return change;
        }

        @Override public Properties.Immutable<I, K, V> freeze() {
            return new Properties.Immutable<>(keys.freeze(), values.freeze());
        }

        public static <I, K, V> Properties.Transient<I, K, V> of() {
            return new Properties.Transient<>(SetMultimap.Transient.of(), Map.Transient.of());
        }

    }

}