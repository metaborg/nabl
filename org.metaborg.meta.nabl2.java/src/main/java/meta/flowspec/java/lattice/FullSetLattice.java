package meta.flowspec.java.lattice;

import java.util.Collection;
import java.util.Iterator;

import org.pcollections.Empty;
import org.pcollections.PSet;

public class FullSetLattice<E> implements CompleteLattice<PSet<E>> {

    @SuppressWarnings("unchecked")
    @Override
    public PSet<E> top() {
        return TOP;
    }

    @Override
    public PSet<E> bottom() {
        return Empty.set();
    }

    @Override
    public boolean lte(PSet<E> one, PSet<E> other) {
        if (other == TOP) {
            return true;
        } else if (one == TOP) {
            return false;
        } else {
            return other.containsAll(one);
        }
    }

    @Override
    public PSet<E> glb(PSet<E> one, PSet<E> other) {
        if (other == TOP) {
            return one;
        } else if (one == TOP) {
            return other;
        } else {
            return one.minusAll(one.minusAll(other));
        }
    }

    @Override
    public PSet<E> lub(PSet<E> one, PSet<E> other) {
        if (one == TOP || other == TOP) {
            return this.top();
        } else {
            return one.plusAll(other);
        }
    }

    @SuppressWarnings("rawtypes")
    public static final PSet TOP = new PSet() {

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public Iterator<Object> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray(Object[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection c) {
            return true;
        }

        @Override
        public PSet plus(Object e) {
            return this;
        }

        @Override
        public PSet plusAll(Collection list) {
            return this;
        }

//        @SuppressWarnings("unchecked")
        @Override
        public PSet minus(Object e) {
            throw new UnsupportedOperationException();
//            return new Complement(OrderedPSet.singleton(e));
        }

//        @SuppressWarnings("unchecked")
        @Override
        public PSet<Object> minusAll(Collection list) {
            throw new UnsupportedOperationException();
//            return new Complement(OrderedPSet.from(list));
        }

        @Override
        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

    };
}
