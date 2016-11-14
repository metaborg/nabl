package org.metaborg.meta.nabl2.regexp;


public abstract class ARegExpFunction<S, T> implements IRegExpFunction<S,T> {

    @Override public T emptySet() {
        return defaultValue();
    }

    @Override public T emptyString() {
        return defaultValue();
    }

    @Override public T symbol(S symbol) {
        return defaultValue();
    }

    @Override public T concat(IRegExp<S> left, IRegExp<S> right) {
        return defaultValue();
    }

    @Override public T closure(IRegExp<S> re) {
        return defaultValue();
    }

    @Override public T or(IRegExp<S> left, IRegExp<S> right) {
        return defaultValue();
    }

    @Override public T and(IRegExp<S> left, IRegExp<S> right) {
        return defaultValue();
    }

    @Override public T complement(IRegExp<S> re) {
        return defaultValue();
    }

    public abstract T defaultValue();

}
