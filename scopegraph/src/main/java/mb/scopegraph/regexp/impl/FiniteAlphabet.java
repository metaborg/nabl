package mb.scopegraph.regexp.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.metaborg.util.collection.ImList;

import mb.scopegraph.regexp.IAlphabet;

public final class FiniteAlphabet<S> implements IAlphabet<S>, Serializable {

    private static final long serialVersionUID = 42L;

    private final ImList.Immutable<S> symbols;

    @SafeVarargs public FiniteAlphabet(S... symbols) {
        this(new HashSet<>(Arrays.asList(symbols)));
    }

    public FiniteAlphabet(List<S> alphabet) {
        this(new HashSet<>(alphabet));
    }

    public FiniteAlphabet(Set<S> alphabet) {
        this.symbols = ImList.Immutable.copyOf(alphabet);
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

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + symbols.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") final FiniteAlphabet<S> other = (FiniteAlphabet<S>) obj;
        if(!symbols.equals(other.symbols))
            return false;
        return true;
    }
    
    @Override public String toString() {
        return symbols.toString();
    }
    
}