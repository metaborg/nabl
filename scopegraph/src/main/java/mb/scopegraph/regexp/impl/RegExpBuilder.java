package mb.scopegraph.regexp.impl;

import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpBuilder;

public final class RegExpBuilder<S> implements IRegExpBuilder<S> {

    @Override public IRegExp<S> emptySet() {
        return EmptySet.of();
    }

    @Override public IRegExp<S> emptyString() {
        return EmptyString.of();
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
