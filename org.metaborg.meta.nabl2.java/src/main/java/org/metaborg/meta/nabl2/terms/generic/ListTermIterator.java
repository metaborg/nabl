package org.metaborg.meta.nabl2.terms.generic;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ListTerms;

public class ListTermIterator implements Iterator<ITerm> {

    private IListTerm current;

    public ListTermIterator(IListTerm list) {
        this.current = list;
    }

    @Override public boolean hasNext() {
        return current.match(ListTerms.cases(cons -> true, nil -> false, var -> {
            throw new IllegalStateException("Cannot iterate over a non-ground list.");
        }));
    }

    @Override public ITerm next() {
        return current.match(ListTerms.cases(cons -> {
            current = cons.getTail();
            return cons.getHead();
        }, nil -> {
            throw new NoSuchElementException();
        }, var -> {
            throw new IllegalStateException("Cannot iterate over a non-ground list.");
        }));
    }

}