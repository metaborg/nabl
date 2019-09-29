package mb.nabl2.util.collections;

import java.util.stream.Collectors;

import org.metaborg.util.functions.Action2;

import io.usethesource.capsule.Map;

public abstract class MultiSet<E> {

    protected abstract Map<E, Integer> elements();

    public boolean isEmpty() {
        return elements().isEmpty();
    }

    public int size() {
        return elements().values().stream().mapToInt(i -> i).sum();
    }

    public int count(E e) {
        return elements().getOrDefault(e, 0);
    }

    public java.util.Set<E> elementSet() {
        return elements().keySet();
    }

    public void forEach(Action2<E, Integer> f) {
        elements().entrySet().forEach(e -> f.apply(e.getKey(), e.getValue()));
    }

    public static class Immutable<E> extends MultiSet<E> {

        private final Map.Immutable<E, Integer> elements;

        private Immutable(Map.Immutable<E, Integer> elements) {
            this.elements = elements;
        }

        @Override protected Map<E, Integer> elements() {
            return elements;
        }

        public Immutable<E> add(E e) {
            return add(e, 1);
        }

        public Immutable<E> add(E e, int n) {
            if(n < 0) {
                throw new IllegalArgumentException("count must be positive");
            }
            final int c = elements.getOrDefault(e, 0) + n;
            if(c > 0) {
                return new MultiSet.Immutable<>(elements.__put(e, c));
            } else {
                return new MultiSet.Immutable<>(elements.__remove(e));
            }
        }

        public Immutable<E> remove(E e) {
            return remove(e, 1);
        }

        public Immutable<E> remove(E e, int n) {
            if(n < 0) {
                throw new IllegalArgumentException("count must be positive");
            }
            final int c = Math.max(0, elements.getOrDefault(e, 0) - n);
            if(c > 0) {
                return new MultiSet.Immutable<>(elements.__put(e, c));
            } else {
                return new MultiSet.Immutable<>(elements.__remove(e));
            }

        }

        public Immutable<E> removeAll(E e) {
            return new MultiSet.Immutable<>(elements.__remove(e));
        }

        public MultiSet.Transient<E> melt() {
            return new MultiSet.Transient<>(elements.asTransient());
        }

        public static <E> MultiSet.Immutable<E> of() {
            return new MultiSet.Immutable<>(Map.Immutable.of());
        }

    }

    public static class Transient<E> extends MultiSet<E> {

        private Map.Transient<E, Integer> elements;

        private Transient(Map.Transient<E, Integer> elements) {
            this.elements = elements;
        }

        @Override protected Map<E, Integer> elements() {
            return elements;
        }

        /*
         * @return New count
         */
        public int add(E e) {
            return add(e, 1);
        }

        /*
         * @return New count
         */
        public int add(E e, int n) {
            if(n < 0) {
                throw new IllegalArgumentException("count must be positive");
            }
            final int c = elements.getOrDefault(e, 0) + n;
            if(c > 0) {
                elements.__put(e, c);
            } else {
                elements.__remove(e);
            }
            return c;
        }

        /*
         * @return New count
         */
        public int remove(E e) {
            return remove(e, 1);
        }

        /*
         * @return New count
         */
        public int remove(E e, int n) {
            if(n < 0) {
                throw new IllegalArgumentException("count must be positive");
            }
            final int c = Math.max(0, elements.getOrDefault(e, 0) - n);
            if(c > 0) {
                elements.__put(e, c);
            } else {
                elements.__remove(e);
            }
            return c;
        }

        /*
         * @return Old count
         */
        public int removeAll(E e) {
            final int c = elements.getOrDefault(e, 0);
            elements.__remove(e);
            return c;
        }

        public MultiSet.Immutable<E> freeze() {
            return new MultiSet.Immutable<>(elements.freeze());
        }

        public static <E> MultiSet.Transient<E> of() {
            return new MultiSet.Transient<>(Map.Transient.of());
        }


    }

    @Override public String toString() {
        return elements().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

}