package org.metaborg.meta.nabl2.transitiveclosure;

import org.metaborg.meta.nabl2.collections.pcollections.HashTreePMultimap;
import org.metaborg.meta.nabl2.collections.pcollections.PMultimap;
import org.pcollections.PSet;

public class TransitiveClosure<T> {

    private final PMultimap<T,T> smaller;
    private final PMultimap<T,T> larger;

    public TransitiveClosure() {
        this.smaller = new HashTreePMultimap<>();
        this.larger = new HashTreePMultimap<>();
    }

    private TransitiveClosure(PMultimap<T,T> smaller, PMultimap<T,T> larger) {
        this.smaller = smaller;
        this.larger = larger;
    }

    public TransitiveClosure<T> add(T first, T second) throws SymmetryException {
        if (larger.get(second).contains(first)) {
            throw new SymmetryException();
        }

        PSet<T> largerValues = larger.get(second).plus(second); // b' >= b
        PMultimap<T,T> newLarger = larger;
        for (T t : smaller.get(first).plus(first)) {
            newLarger = newLarger.plusAll(t, largerValues);
        }
        newLarger = newLarger.plus(first, second);

        PSet<T> smallerValues = smaller.get(first).plus(first); // a' =< a
        PMultimap<T,T> newSmaller = smaller;
        for (T t : larger.get(second).plus(second)) {
            newSmaller = newSmaller.plusAll(t, smallerValues);
        }
        newSmaller = newSmaller.plus(second, first);

        return new TransitiveClosure<>(newSmaller, newLarger);
    }

    public PSet<T> smaller(T elem) {
        return smaller.get(elem);
    }

    public PSet<T> larger(T elem) {
        return larger.get(elem);
    }

    public boolean contains(T first, T second) {
        return larger.get(first).contains(second);
    }

}