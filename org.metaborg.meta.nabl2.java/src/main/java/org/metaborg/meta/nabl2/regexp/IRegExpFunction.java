package org.metaborg.meta.nabl2.regexp;

public interface IRegExpFunction<S, T> {

    T emptySet();

    T emptyString();

    T symbol(S s);

    T concat(IRegExp<S> left, IRegExp<S> right);

    T closure(IRegExp<S> re);

    T or(IRegExp<S> left, IRegExp<S> right);

    T and(IRegExp<S> left, IRegExp<S> right);

    T complement(IRegExp<S> re);

}