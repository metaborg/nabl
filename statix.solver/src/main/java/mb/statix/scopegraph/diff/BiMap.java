package mb.statix.scopegraph.diff;

import java.util.Map.Entry;
import java.util.Set;

import io.usethesource.capsule.Map;

public abstract class BiMap<E> {

    public abstract boolean containsKey(E key);

    public abstract boolean containsValue(E value);

    public abstract boolean containsEntry(E key, E value);

    public abstract Set<E> keySet();

    public abstract Set<E> valueSet();

    public abstract Set<Map.Entry<E, E>> entrySet();

    public static class Immutable<E> extends BiMap<E> {

        private final Map.Immutable<E, E> fwd;
        private final Map.Immutable<E, E> bwd;

        private Immutable(Map.Immutable<E, E> fwd, Map.Immutable<E, E> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public boolean containsKey(E key) {
            return fwd.containsKey(key);
        }

        @Override public boolean containsValue(E value) {
            return bwd.containsKey(value);
        }

        @Override public boolean containsEntry(E key, E value) {
            return fwd.containsKey(key) && fwd.get(key).equals(value);
        }

        @Override public Set<E> keySet() {
            return fwd.keySet();
        }

        @Override public Set<E> valueSet() {
            return bwd.keySet();
        }

        @Override public Set<Entry<E, E>> entrySet() {
            return fwd.entrySet();
        }

        public Transient<E> melt() {
            return new Transient<>(fwd.asTransient(), bwd.asTransient());
        }

        @Override public String toString() {
            return fwd.toString();
        }

        public static <E> Immutable<E> of() {
            return new Immutable<>(Map.Immutable.of(), Map.Immutable.of());
        }

        public static <E> Immutable<E> of(E key, E value) {
            return new Immutable<>(Map.Immutable.of(key, value), Map.Immutable.of(value, key));
        }

    }

    public static class Transient<E> extends BiMap<E> {

        private final Map.Transient<E, E> fwd;
        private final Map.Transient<E, E> bwd;

        private Transient(Map.Transient<E, E> fwd, Map.Transient<E, E> bwd) {
            this.fwd = fwd;
            this.bwd = bwd;
        }

        @Override public boolean containsKey(E key) {
            return fwd.containsKey(key);
        }

        @Override public boolean containsValue(E value) {
            return bwd.containsKey(value);
        }

        @Override public boolean containsEntry(E key, E value) {
            return fwd.containsKey(key) && fwd.get(key).equals(value);
        }

        @Override public Set<E> keySet() {
            return fwd.keySet();
        }

        @Override public Set<E> valueSet() {
            return bwd.keySet();
        }

        @Override public Set<Entry<E, E>> entrySet() {
            return fwd.entrySet();
        }

        public boolean canPut(E key, E value) {
            if(fwd.containsKey(key) && !fwd.get(key).equals(value)) {
                return false;
            }
            if(bwd.containsKey(value) && !bwd.get(value).equals(key)) {
                return false;
            }
            return true;
        }

        public void put(E key, E value) {
            if(!canPut(key, value)) {
                throw new IllegalArgumentException("Key or value already set.");
            }
            fwd.__put(key, value);
            bwd.__put(value, key);
        }

        public void putAll(BiMap<E> other) {
            putAll(other.entrySet());
        }

        public void putAll(Iterable<Entry<E, E>> entries) {
            entries.forEach(e -> put(e.getKey(), e.getValue()));
        }

        public Immutable<E> freeze() {
            return new Immutable<>(fwd.freeze(), bwd.freeze());
        }

        @Override public String toString() {
            return fwd.toString();
        }

        public static <E> Transient<E> of() {
            return new Transient<>(Map.Transient.of(), Map.Transient.of());
        }

        public static <E> Transient<E> of(E key, E value) {
            return new Transient<>(Map.Transient.of(key, value), Map.Transient.of(value, key));
        }

    }

}