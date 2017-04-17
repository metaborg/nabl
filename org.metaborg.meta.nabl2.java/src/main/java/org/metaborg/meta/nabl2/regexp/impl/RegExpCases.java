package org.metaborg.meta.nabl2.regexp.impl;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.metaborg.meta.nabl2.regexp.IRegExp;

class RegExpCases<S, T> {

    private Supplier<T> onEmptySet;
    private Supplier<T> onEmptyString;
    private Function<S, T> onSymbol;
    private BiFunction<? super IRegExp<S>, ? super IRegExp<S>, T> onConcat;
    private Function<? super IRegExp<S>, T> onClosure;
    private BiFunction<? super IRegExp<S>, ? super IRegExp<S>, T> onOr;
    private BiFunction<? super IRegExp<S>, ? super IRegExp<S>, T> onAnd;
    private Function<? super IRegExp<S>, T> onComplement;

    RegExpCases<S, T> emptySet(Supplier<T> onEmptySet) {
        this.onEmptySet = onEmptySet;
        return this;
    }

    RegExpCases<S, T> emptyString(Supplier<T> onEmptyString) {
        this.onEmptyString = onEmptyString;
        return this;
    }

    RegExpCases<S, T> symbol(Function<S, T> onSymbol) {
        this.onSymbol = onSymbol;
        return this;
    }

    RegExpCases<S, T> concat(BiFunction<? super IRegExp<S>, ? super IRegExp<S>, T> onConcat) {
        this.onConcat = onConcat;
        return this;
    }

    RegExpCases<S, T> closure(Function<? super IRegExp<S>, T> onClosure) {
        this.onClosure = onClosure;
        return this;
    }

    RegExpCases<S, T> or(BiFunction<? super IRegExp<S>, ? super IRegExp<S>, T> onOr) {
        this.onOr = onOr;
        return this;
    }

    RegExpCases<S, T> and(BiFunction<? super IRegExp<S>, ? super IRegExp<S>, T> onAnd) {
        this.onAnd = onAnd;
        return this;
    }

    RegExpCases<S, T> complement(Function<? super IRegExp<S>, T> onComplement) {
        this.onComplement = onComplement;
        return this;
    }

    IRegExp.ICases<S, T> otherwise(Supplier<T> otherwise) {
        return new IRegExp.ICases<S, T>() {

            @Override public T emptySet() {
                if(onEmptySet != null) {
                    return onEmptySet.get();
                }
                return otherwise.get();
            }

            @Override public T emptyString() {
                if(onEmptyString != null) {
                    return onEmptyString.get();
                }
                return otherwise.get();
            }

            @Override public T symbol(S s) {
                if(onSymbol != null) {
                    return onSymbol.apply(s);
                }
                return otherwise.get();
            }

            @Override public T concat(IRegExp<S> left, IRegExp<S> right) {
                if(onConcat != null) {
                    return onConcat.apply(left, right);
                }
                return otherwise.get();
            }

            @Override public T closure(IRegExp<S> re) {
                if(onClosure != null) {
                    return onClosure.apply(re);
                }
                return otherwise.get();
            }

            @Override public T or(IRegExp<S> left, IRegExp<S> right) {
                if(onOr != null) {
                    return onOr.apply(left, right);
                }
                return otherwise.get();
            }

            @Override public T and(IRegExp<S> left, IRegExp<S> right) {
                if(onAnd != null) {
                    return onAnd.apply(left, right);
                }
                return otherwise.get();
            }

            @Override public T complement(IRegExp<S> re) {
                if(onComplement != null) {
                    return onComplement.apply(re);
                }
                return otherwise.get();
            }

            @Override public T apply(IRegExp<S> t) {
                return t.match(this);
            }

        };
    }

    static <S, T> IRegExp.ICases<S, T> of(
            // @formatter:off
            Supplier<T> onEmptySet,
            Supplier<T> onEmptyString,
            Function<S,T> onSymbol,
            BiFunction<? super IRegExp<S>,? super IRegExp<S>,T> onConcat,
            Function<? super IRegExp<S>,T> onClosure,
            BiFunction<? super IRegExp<S>,? super IRegExp<S>,T> onOr,
            BiFunction<? super IRegExp<S>,? super IRegExp<S>,T> onAnd,
            Function<? super IRegExp<S>,T> onComplement
            // @formatter:on
    ) {
        return new IRegExp.ICases<S, T>() {

            @Override public T emptySet() {
                return onEmptySet.get();
            }

            @Override public T emptyString() {
                return onEmptyString.get();
            }

            @Override public T symbol(S s) {
                return onSymbol.apply(s);
            }

            @Override public T concat(IRegExp<S> left, IRegExp<S> right) {
                return onConcat.apply(left, right);
            }

            @Override public T closure(IRegExp<S> re) {
                return onClosure.apply(re);
            }

            @Override public T or(IRegExp<S> left, IRegExp<S> right) {
                return onOr.apply(left, right);
            }

            @Override public T and(IRegExp<S> left, IRegExp<S> right) {
                return onAnd.apply(left, right);
            }

            @Override public T complement(IRegExp<S> re) {
                return onComplement.apply(re);
            }

            @Override public T apply(IRegExp<S> t) {
                return t.match(this);
            }

        };
    }

}