package mb.nabl2.regexp.impl;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpBuilder;

public final class RegExpBuilder<S> implements IRegExpBuilder<S> {

    @Override public IRegExp<S> emptySet() {
        return new ImmutableEmptySet<>();
    }

    @Override public IRegExp<S> emptyString() {
        return new ImmutableEmptyString<>();
    }

    @Override public IRegExp<S> symbol(S s) {
        return ImmutableSymbol.of(s);
    }

    @Override public IRegExp<S> concat(final IRegExp<S> left, final IRegExp<S> right) {
        return ImmutableConcat.of(left, right);
    }

    @Override public IRegExp<S> closure(final IRegExp<S> re) {
        return ImmutableClosure.of(re);
    }

    @Override public IRegExp<S> or(final IRegExp<S> left, final IRegExp<S> right) {
        return ImmutableOr.of(left, right);
    }

    @Override public IRegExp<S> and(final IRegExp<S> left, final IRegExp<S> right) {
        return ImmutableAnd.of(left, right);
    }

    @Override public IRegExp<S> complement(final IRegExp<S> re) {
        return ImmutableComplement.of(re);
    }

}