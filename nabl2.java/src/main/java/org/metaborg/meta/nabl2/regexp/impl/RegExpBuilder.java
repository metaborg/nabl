package org.metaborg.meta.nabl2.regexp.impl;

import java.io.Serializable;
import java.util.function.Supplier;

import org.metaborg.meta.nabl2.regexp.IAlphabet;
import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.regexp.IRegExpBuilder;

public final class RegExpBuilder<S> implements IRegExpBuilder<S>, Serializable {

    private static final long serialVersionUID = 42L;

    private final IAlphabet<S> alphabet;

    public RegExpBuilder(IAlphabet<S> alphabet) {
        this.alphabet = alphabet;
    }

    @Override public IAlphabet<S> getAlphabet() {
        return alphabet;
    }

    @Override public IRegExp<S> emptySet() {
        return ImmutableEmptySet.of(this);
    }

    @Override public IRegExp<S> emptyString() {
        return ImmutableEmptyString.of(this);
    }

    @Override public IRegExp<S> symbol(S s) {
        return ImmutableSymbol.of(s, this);
    }

    @Override public IRegExp<S> concat(final IRegExp<S> left, final IRegExp<S> right) {
        // @formatter:off
        return left.match(new RegExpCases<S,IRegExp<S>>()
                .emptySet(() -> emptySet())
                .emptyString(() -> right)
                .concat((innerLeft,innerRight) -> concat(innerLeft, concat(innerRight, right)))
                .otherwise(() -> right.match(new RegExpCases<S,IRegExp<S>>()
                        .emptySet(() -> emptySet())
                        .emptyString(() -> left)
                        .otherwise(() -> ImmutableConcat.of(left, right, this)))));
        // @formatter:on
    }

    @Override public IRegExp<S> closure(final IRegExp<S> re) {
        // @formatter:off
        return re.match(new RegExpCases<S,IRegExp<S>>()
                .emptySet(() -> emptyString())
                .emptyString(() -> emptyString())
                .closure((innerRe) -> ImmutableClosure.of(innerRe, this))
                .otherwise(() -> ImmutableClosure.of(re, this)));
        // @formatter:on
    }

    @Override public IRegExp<S> or(final IRegExp<S> left, final IRegExp<S> right) {
        if(left.equals(right)) {
            return left;
        }
        // @formatter:off
        Supplier<IRegExp<S>> otherwise = () -> right.match(new RegExpCases<S,IRegExp<S>>()
                .or((innerLeft, innerRight) ->
                        (compare(left, innerLeft) > 0)
                                ? or(innerLeft, or(left, innerRight))
                                : ImmutableOr.of(left, ImmutableOr.of(innerLeft, innerRight, this), this))
                .otherwise(() -> {
                    return (compare(left, right) > 0) ? or(right, left) : ImmutableOr.of(left, right, this);
                }));
        return left.match(new RegExpCases<S,IRegExp<S>>()
                .emptySet(() -> right)
                .or((innerLeft, innerRight) -> or(innerLeft, or(innerRight, right)))
                .complement(innerRe -> innerRe.match(new RegExpCases<S,IRegExp<S>>()
                        .emptySet(() -> left)
                        .otherwise(otherwise)))
                .otherwise(otherwise));
        // @formatter:on
    }

    @Override public IRegExp<S> and(final IRegExp<S> left, final IRegExp<S> right) {
        if(left.equals(right)) {
            return left;
        }
        // @formatter:off
        Supplier<IRegExp<S>> otherwise = () -> right.match(new RegExpCases<S,IRegExp<S>>()
                .and((innerLeft, innerRight) ->
                        (compare(left, innerLeft) > 0)
                                ? and(innerLeft, and(left, innerRight))
                                : ImmutableAnd.of(left, ImmutableAnd.of(innerLeft, innerRight, this), this))
                .otherwise(() -> {
                    return (compare(left, right) > 0) ? and(right, left) : ImmutableAnd.of(left, right, this);
                }));
        return left.match(new RegExpCases<S,IRegExp<S>>()
                .emptySet(() -> emptySet())
                .and((innerLeft, innerRight) -> and(innerLeft, and(innerRight, right)))
                .complement(innerRe -> innerRe.match(new RegExpCases<S,IRegExp<S>>()
                        .emptySet(() -> right)
                        .otherwise(otherwise)))
                .otherwise(otherwise));
        // @formatter:on
    }

    @Override public IRegExp<S> complement(final IRegExp<S> re) {
        // @formatter:off
        return re.match(new RegExpCases<S,IRegExp<S>>()
                .complement(innerRe -> innerRe)
                .otherwise(() -> ImmutableComplement.of(re, this)));
        // @formatter:on
    }

    private int compare(final IRegExp<S> re1, final IRegExp<S> re2) {
        // @formatter:off
        Supplier<Integer> defaultValue = () -> (order(re1) - order(re2));
        return re1.match(new RegExpCases<S,Integer>()
                .concat((left1, right1) -> re2.match(new RegExpCases<S,Integer>()
                        .concat((left2, right2) -> {
                            int c = compare(left1, left2);
                            if (c == 0) {
                                c = compare(right1, right2);
                            }
                            return c;
                        })
                        .otherwise(defaultValue)))
                .closure(innerRe1 -> re2.match(new RegExpCases<S,Integer>()
                        .closure(innerRe2 -> compare(innerRe1, innerRe2))
                        .otherwise(defaultValue)))
                .or((left1, right1) -> re2.match(new RegExpCases<S,Integer>()
                        .or((left2, right2) -> {
                            int c = compare(left1, left2);
                            if (c == 0) {
                                c = compare(right1, right2);
                            }
                            return c;
                        })
                        .otherwise(defaultValue)))
                .and((left1, right1) -> re2.match(new RegExpCases<S,Integer>()
                        .and((left2, right2) -> {
                            int c = compare(left1, left2);
                            if (c == 0) {
                                c = compare(right1, right2);
                            }
                            return c;
                        })
                        .otherwise(defaultValue)))
                .complement(innerRe1 -> re2.match(new RegExpCases<S,Integer>()
                        .complement(innerRe2 -> compare(innerRe1, innerRe2))
                        .otherwise(defaultValue)))
                .otherwise(defaultValue));
        // @formatter:on
    }

    private int order(IRegExp<S> re) {
        // @formatter:off
        return re.match(RegExpCases.<S,Integer>of(
            ()            -> 1,
            ()            -> 2,
            (s)           -> (8 + alphabet.indexOf(s)),
            (left, right) -> 4,
            (innerRe)     -> 5,
            (left, right) -> 6,
            (left, right) -> 6,
            (innerRe)     -> 7
        ));
        // @formatter:on
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + alphabet.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") final RegExpBuilder<S> other = (RegExpBuilder<S>) obj;
        if(!alphabet.equals(other.alphabet))
            return false;
        return true;
    }
}