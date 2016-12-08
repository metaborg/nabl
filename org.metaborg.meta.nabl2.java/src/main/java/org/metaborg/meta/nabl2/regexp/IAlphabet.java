package org.metaborg.meta.nabl2.regexp;


public interface IAlphabet<S> extends Iterable<S> {

    boolean contains(S s);

    int indexOf(S s);

}