package org.metaborg.meta.nabl2.regexp;

public class Deriver<S> implements IRegExp.ICases<S, IRegExp<S>> {

    private final S symbol;
    private final IRegExpBuilder<S> builder;

    public Deriver(S symbol, IRegExpBuilder<S> builder) {
        this.symbol = symbol;
        this.builder = builder;
    }

    public S getSymbol() {
        return symbol;
    }

    @Override public IRegExp<S> emptySet() {
        return builder.emptySet();
    }

    @Override public IRegExp<S> emptyString() {
        return builder.emptySet();
    }

    @Override public IRegExp<S> symbol(S s) {
        if(s.equals(symbol)) {
            return builder.emptyString();
        } else {
            return builder.emptySet();
        }
    }

    @Override public IRegExp<S> concat(IRegExp<S> left, IRegExp<S> right) {
        IRegExp<S> newLeft = builder.concat(left.match(this), right);
        if(left.isNullable()) {
            return builder.or(newLeft, right.match(this));
        } else {
            return newLeft;
        }
    }

    @Override public IRegExp<S> closure(IRegExp<S> re) {
        return builder.concat(re.match(this), builder.closure(re));
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
