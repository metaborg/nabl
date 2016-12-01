package org.metaborg.meta.nabl2.regexp;

import java.util.function.Supplier;

public final class RegExpBuilder<S> implements IRegExpBuilder<S> {

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
        if (left.equals(right)) {
            return left;
        }
        if (compare(left, right) > 0) {
            return or(right, left);
        }
        // @formatter:off
        return left.match(new RegExpCases<S,IRegExp<S>>()
                .or((innerLeft,innerRight) -> ImmutableOr.of(innerLeft, or(innerRight, right), this))
                .otherwise(() -> left.match(new RegExpCases<S,IRegExp<S>>()
                        .emptySet(() -> right)
                        .complement(innerRe -> innerRe.match(new RegExpCases<S,IRegExp<S>>()
                                .emptySet(() -> left)
                                .otherwise(() -> ImmutableOr.of(left, right, this))))
                        .otherwise(() -> ImmutableOr.of(left, right, this)))));
        // @formatter:on
    }

    @Override public IRegExp<S> and(final IRegExp<S> left, final IRegExp<S> right) {
        if (left.equals(right)) {
            return left;
        }
        if (compare(left, right) > 0) {
            return and(right, left);
        }
        // @formatter:off
        return left.match(new RegExpCases<S,IRegExp<S>>()
                .and((innerLeft,innerRight) -> ImmutableAnd.of(innerLeft, and(innerRight, right), this))
                .otherwise(() -> left.match(new RegExpCases<S,IRegExp<S>>()
                        .emptySet(() -> emptySet())
                        .complement(innerRe -> innerRe.match(new RegExpCases<S,IRegExp<S>>()
                                .emptySet(() -> right)
                                .otherwise(() -> ImmutableAnd.of(left, right, this))))
                        .otherwise(() -> ImmutableAnd.of(left, right, this)))));
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
                .symbol(s1 -> re2.match(new RegExpCases<S,Integer>()
                        .symbol(s2 -> (alphabet.indexOf(s1) - alphabet.indexOf(s2)))
                        .otherwise(defaultValue)))
                .concat((left1,right1) -> re2.match(new RegExpCases<S,Integer>()
                        .concat((left2,right2) -> {
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
                .or((left1,right1) -> re2.match(new RegExpCases<S,Integer>()
                        .or((left2,right2) -> {
                            int c = compare(left1, left2);
                            if (c == 0) {
                                c = compare(right1, right2);
                            }
                            return c;
                        })
                        .otherwise(defaultValue)))
                .and((left1,right1) -> re2.match(new RegExpCases<S,Integer>()
                        .and((left2,right2) -> {
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
            () -> 1,
            () -> 2,
            (s) -> (7 + alphabet.indexOf(s)),
            (left,right) -> 5,
            (innerRe) -> 4,
            (left,right) -> Math.min(order(left), order(right)),
            (left,right) -> Math.min(order(left), order(right)),
            (innerRe) -> 3
        ));
        // @formatter:on
    }

}