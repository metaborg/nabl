package org.metaborg.meta.nabl2.regexp;


public interface IRegExpBuilder<S> {

    IAlphabet<S> getAlphabet();

    IRegExp<S> emptySet();

    IRegExp<S> emptyString();

    IRegExp<S> symbol(S s);

    IRegExp<S> concat(IRegExp<S> left, IRegExp<S> right);

    IRegExp<S> closure(IRegExp<S> re);

    IRegExp<S> or(IRegExp<S> left, IRegExp<S> right);

    IRegExp<S> and(IRegExp<S> left, IRegExp<S> right);

    IRegExp<S> complement(IRegExp<S> re);

}