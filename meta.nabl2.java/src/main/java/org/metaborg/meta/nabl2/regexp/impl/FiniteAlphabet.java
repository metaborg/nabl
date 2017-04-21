package org.metaborg.meta.nabl2.regexp.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.metaborg.meta.nabl2.regexp.IAlphabet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public final class FiniteAlphabet<S> implements IAlphabet<S>, Serializable {

    private static final long serialVersionUID = 42L;

    private final ImmutableList<S> symbols;

    @SafeVarargs public FiniteAlphabet(S... symbols) {
        this(Arrays.asList(symbols));
    }

    public FiniteAlphabet(Collection<S> alphabet) {
        this.symbols = ImmutableList.copyOf(ImmutableSet.copyOf(alphabet));
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