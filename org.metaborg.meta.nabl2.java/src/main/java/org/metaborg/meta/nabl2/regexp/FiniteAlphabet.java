package org.metaborg.meta.nabl2.regexp;

import java.util.Iterator;

import com.google.common.collect.ImmutableList;

public final class FiniteAlphabet<S> implements IAlphabet<S> {

    private final ImmutableList<S> symbols;

    public FiniteAlphabet(Iterable<S> alphabet) {
        this.symbols = ImmutableList.copyOf(alphabet);
    }

    @Override public Iterator<S> iterator() {
        return symbols.iterator();
    }

    @Override public boolean contains(S s) {
        return symbols.contains(s);
    }

    @Override public int indexOf(S s) {
        if (!contains(s)) {
            throw new IllegalArgumentException("Symbol not in alphabet.");
        }
        return symbols.indexOf(s);
    }

}
