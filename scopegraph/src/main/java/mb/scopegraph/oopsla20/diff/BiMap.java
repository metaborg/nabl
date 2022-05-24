package mb.scopegraph.oopsla20.diff;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

import io.usethesource.capsule.Map;

public abstract class BiMap<E> {

    public abstract boolean containsKey(E key);

    public abstract boolean containsValue(E value);

    public abstract boolean containsEntry(E key, E value);

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract Set<E> keySet();

    public abstract Set<E> valueSet();

    public abstract Set<Map.Entry<E, E>> entrySet();

    public static class Immutable<E> extends BiMap<E> implements Serializable {

        private static final long serialVersionUID = 42L;

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

        @Override public int size() {
            return fwd.size();
        }

        @Override public boolean isEmpty() {
            return fwd.isEmpty();
        }

        public E getKey(E key) {
            return fwd.get(key);
        }

        public E getValue(E value) {
            return bwd.get(value);
        }

        public E getKeyOrDefault(E key, E def) {
            return fwd.getOrDefault(key, def);
        }

        public E getValueOrDefault(E value, E def) {
            return bwd.getOrDefault(value, def);
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

        public Immutable<E> putAll(BiMap<E> other) {
            final Transient<E> newMap = this.melt();
            newMap.putAll(other);
            return newMap.freeze();
        }

        public Transient<E> melt() {
            return new Transient<>(fwd.asTransient(), bwd.asTransient());
        }

        public Map.Immutable<E, E> asMap() {
            return fwd;
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

        public static <E> Immutable<E> from(BiMap<E> other) {
            Transient<E> newMap = BiMap.Transient.of();
            newMap.putAll(other);
            return newMap.freeze();
        }

        @SuppressWarnings("unchecked") @Override public boolean equals(Object obj) {
            if(obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            return fwd.equals(((BiMap.Immutable<E>) obj).fwd);
        }

        @Override public int hashCode() {
            return fwd.hashCode();
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

        public Immutable<E> put(E key, E value) {
            if(!canPut(key, value)) {
                throw new IllegalArgumentException("Key or value already set.");
            }
            return new Immutable<>(fwd.__put(key, value), bwd.__put(value, key));
        }

        public Immutable<E> invert() {
            return new BiMap.Immutable<>(bwd, fwd);
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

        @Override public int size() {
            return fwd.size();
        }

        @Override public boolean isEmpty() {
            return fwd.isEmpty();
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

        public boolean put(E key, E value) {
            if(containsEntry(key, value)) {
                return false;
            }
            if(!canPut(key, value)) {
                throw new IllegalArgumentException("Key or value already set.");
            }
            fwd.__put(key, value);
            bwd.__put(value, key);
            return true;
        }

        public boolean putAll(BiMap<E> other) {
            return putAll(other.entrySet());
        }

        public boolean putAll(Iterable<Entry<E, E>> entries) {
            boolean changed = false;
            for(Entry<E, E> e : entries) {
                changed |= put(e.getKey(), e.getValue());
            }
            return changed;
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

        public E getKey(E key) {
            return fwd.get(key);
        }

        public E getValue(E value) {
            return bwd.get(value);
        }

        public E getKeyOrDefault(E key, E def) {
            return fwd.getOrDefault(key, def);
        }

        public E getValueOrDefault(E value, E def) {
            return bwd.getOrDefault(value, def);
        }

    }

}
