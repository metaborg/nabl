package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

public abstract class Properties<T> implements IProperties<T>, Serializable {
    private static final long serialVersionUID = 42L;

    private final SetMultimap<T, ITerm> keys;
    private final Map<Tuple2<T, ITerm>, ITerm> values;

    private Properties(SetMultimap<T, ITerm> keys, Map<Tuple2<T, ITerm>, ITerm> values) {
        this.keys = keys;
        this.values = values;
    }

    @Override public java.util.Set<T> getIndices() {
        return keys.keySet();
    }

    @Override public Set<ITerm> getDefinedKeys(T index) {
        return keys.get(index);
    }

    @Override public Optional<ITerm> getValue(T index, ITerm key) {
        return Optional.ofNullable(values.get(ImmutableTuple2.of(index, key)));
    }

    public static class Immutable<T> extends Properties<T> implements IProperties.Immutable<T> {
        private static final long serialVersionUID = 42L;

        @SuppressWarnings("unused") private final SetMultimap.Immutable<T, ITerm> keys;
        @SuppressWarnings("unused") private final Map.Immutable<Tuple2<T, ITerm>, ITerm> values;

        private Immutable(SetMultimap.Immutable<T, ITerm> keys, Map.Immutable<Tuple2<T, ITerm>, ITerm> values) {
            super(keys, values);
            this.keys = keys;
            this.values = values;
        }

        public static <T> Properties.Immutable<T> of() {
            return new Properties.Immutable<>(SetMultimap.Immutable.of(), Map.Immutable.of());
        }

    }

    public static class Mutable<T> extends Properties<T> implements IProperties.Mutable<T> {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Transient<T, ITerm> keys;
        private final Map.Transient<Tuple2<T, ITerm>, ITerm> values;

        private Mutable(SetMultimap.Transient<T, ITerm> keys, Map.Transient<Tuple2<T, ITerm>, ITerm> values) {
            super(keys, values);
            this.keys = keys;
            this.values = values;
        }

        @Override public Optional<ITerm> putValue(T index, ITerm key, ITerm value) {
            ITerm prev = values.__put(ImmutableTuple2.of(index, key), value);
            if(!value.equals(prev)) {
                keys.__put(index, key);
                return Optional.ofNullable(prev);
            }
            return Optional.empty();
        }

        @Override public org.metaborg.meta.nabl2.solver.IProperties.Immutable<T> freeze() {
            return new Properties.Immutable<>(keys.freeze(), values.freeze());
        }

        public static <T> Properties.Mutable<T> of() {
            return new Properties.Mutable<>(SetMultimap.Transient.of(), Map.Transient.of());
        }

    }

}