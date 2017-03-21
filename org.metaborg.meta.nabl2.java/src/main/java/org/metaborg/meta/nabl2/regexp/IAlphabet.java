package org.metaborg.meta.nabl2.regexp;

import java.util.Collection;

public interface IAlphabet<S> extends Iterable<S> {

    boolean contains(S s);

    int indexOf(S s);

    Collection<S> symbols();
    
}