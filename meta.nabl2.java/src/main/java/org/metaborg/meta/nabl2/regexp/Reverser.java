package org.metaborg.meta.nabl2.regexp;


public class Reverser<S> implements IRegExp.ICases<S, IRegExp<S>> {

    private final IRegExpBuilder<S> builder;

    public Reverser(IRegExpBuilder<S> builder) {
        this.builder = builder;
    }

    @Override public IRegExp<S> emptySet() {
        return builder.emptySet();
    }

    @Override public IRegExp<S> emptyString() {
        return builder.emptyString();
    }

    @Override public IRegExp<S> symbol(S s) {
        return builder.symbol(s);
    }

    @Override public IRegExp<S> concat(IRegExp<S> left, IRegExp<S> right) {
        return builder.concat(right, left);
    }

    @Override public IRegExp<S> closure(IRegExp<S> re) {
        return builder.closure(re.match(this));
    }

    @Override public IRegExp<S> or(IRegExp<S> left, IRegExp<S> right) {
        return builder.or(left.match(this), right.match(this));
    }

    @Override public IRegExp<S> and(IRegExp<S> left, IRegExp<S> right) {
        return builder.and(left.match(this), right.match(this));
    }

    @Override public IRegExp<S> complement(IRegExp<S> re) {
        return builder.complement(re.match(this));
    }

    @Override public IRegExp<S> apply(IRegExp<S> t) {
        return t.match(this);
    }

}