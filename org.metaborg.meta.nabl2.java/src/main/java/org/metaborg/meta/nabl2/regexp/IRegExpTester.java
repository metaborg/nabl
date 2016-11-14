package org.metaborg.meta.nabl2.regexp;

public interface IRegExpTester<S> {

    boolean symbol(S symbol);

    boolean or(IRegExp<S> left, IRegExp<S> right);

    boolean and(IRegExp<S> left, IRegExp<S> right);

    boolean concat(IRegExp<S> left, IRegExp<S> right);

    boolean complement(IRegExp<S> re);

    boolean closure(IRegExp<S> re);

    boolean emptyString();

    boolean emptySet();

}