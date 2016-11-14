package org.metaborg.meta.nabl2.regexp;


public abstract class ARegExpTester<S> implements IRegExpTester<S> {

    @Override public boolean emptySet() {
        return defaultValue();
    }

    @Override public boolean emptyString() {
        return defaultValue();
    }

    @Override public boolean symbol(S symbol) {
        return defaultValue();
    }

    @Override public boolean concat(IRegExp<S> left, IRegExp<S> right) {
        return defaultValue();
    }

    @Override public boolean closure(IRegExp<S> re) {
        return defaultValue();
    }

    @Override public boolean or(IRegExp<S> left, IRegExp<S> right) {
        return defaultValue();
    }

    @Override public boolean and(IRegExp<S> left, IRegExp<S> right) {
        return defaultValue();
    }

    @Override public boolean complement(IRegExp<S> re) {
        return defaultValue();
    }

    public abstract boolean defaultValue();

}
