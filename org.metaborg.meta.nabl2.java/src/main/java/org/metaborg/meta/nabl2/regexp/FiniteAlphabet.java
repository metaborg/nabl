package org.metaborg.meta.nabl2.regexp;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;

public final class FiniteAlphabet<S> implements IAlphabet<S>, Serializable {

    private static final long serialVersionUID = 42L;

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
        if(!contains(s)) {
            throw new IllegalArgumentException("Symbol not in alphabet.");
        }
        return symbols.indexOf(s);
    }

    @Override public List<S> symbols() {
        return symbols;
    }

}
