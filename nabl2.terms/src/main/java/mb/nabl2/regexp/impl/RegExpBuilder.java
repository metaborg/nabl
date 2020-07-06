package mb.nabl2.regexp.impl;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpBuilder;

public final class RegExpBuilder<S> implements IRegExpBuilder<S> {

    @Override public IRegExp<S> emptySet() {
        return new EmptySet<>();
    }

    @Override public IRegExp<S> emptyString() {
        return new EmptyString<>();
    }

    @Override public IRegExp<S> symbol(S s) {
        return Symbol.of(s);
    }

    @Override public IRegExp<S> concat(final IRegExp<S> left, final IRegExp<S> right) {
        return Concat.of(left, right);
    }

    @Override public IRegExp<S> closure(final IRegExp<S> re) {
        return Closure.of(re);
    }

    @Override public IRegExp<S> or(final IRegExp<S> left, final IRegExp<S> right) {
        return Or.of(left, right);
    }

    @Override public IRegExp<S> and(final IRegExp<S> left, final IRegExp<S> right) {
        return And.of(left, right);
    }

    @Override public IRegExp<S> complement(final IRegExp<S> re) {
        return Complement.of(re);
    }

}