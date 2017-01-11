package org.metaborg.meta.nabl2.regexp;

public interface IRegExp<S> {

    IRegExpBuilder<S> getBuilder();

    boolean isNullable();

    <T> T match(IRegExpCases<S,T> cases);

}