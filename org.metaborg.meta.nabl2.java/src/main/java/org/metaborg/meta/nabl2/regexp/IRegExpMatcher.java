package org.metaborg.meta.nabl2.regexp;

public interface IRegExpMatcher<S> {

    IRegExpMatcher<S> match(S symbol);

    IRegExpMatcher<S> match(Iterable<S> symbols);

    boolean isAccepting();

    boolean isFinal();

    boolean isEmpty();

}