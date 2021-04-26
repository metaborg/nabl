package mb.scopegraph.regexp.impl;

import java.util.function.Supplier;

import mb.scopegraph.regexp.IAlphabet;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpBuilder;

public final class RegExpNormalizingBuilder<S> implements IRegExpBuilder<S> {

    private final IAlphabet<S> alphabet;
    private final IRegExpBuilder<S> builder;

    public RegExpNormalizingBuilder(IAlphabet<S> alphabet) {
        this(alphabet, new RegExpBuilder<>());
    }

    public RegExpNormalizingBuilder(IAlphabet<S> alphabet, IRegExpBuilder<S> builder) {
        this.alphabet = alphabet;
        this.builder = builder;
    }

    @Override public IRegExp<S> emptySet() {
        return builder.emptySet();
    }

    @Override public IRegExp<S> emptyString() {
        return builder.emptyString();
    }

    @Override public IRegExp<S> symbol(S s) {
        if(!alphabet.contains(s)) {
            throw new IllegalArgumentException("Encountered unknown symbol " + s);
        }
        return builder.symbol(s);
    }

    @Override public IRegExp<S> concat(final IRegExp<S> _left, final IRegExp<S> _right) {
        final IRegExp<S> left = _left.match(this);
        final IRegExp<S> right = _right.match(this);
        // @formatter:off
        return left.match(new RegExpCases<S,IRegExp<S>>()
                .emptySet(() -> emptySet())
                .emptyString(() -> right)
                .concat((innerLeft,innerRight) -> concat(innerLeft, concat(innerRight, right)))
                .otherwise(() -> right.match(new RegExpCases<S,IRegExp<S>>()
                        .emptySet(() -> emptySet())
                        .emptyString(() -> left)
                        .otherwise(() -> builder.concat(left, right)))));
        // @formatter:on
    }

    @Override public IRegExp<S> closure(final IRegExp<S> _re) {
        final IRegExp<S> re = _re.match(this);
        // @formatter:off
        return re.match(new RegExpCases<S,IRegExp<S>>()
                .emptySet(() -> emptyString())
                .emptyString(() -> emptyString())
                .closure((innerRe) -> builder.closure(innerRe))
                .otherwise(() -> builder.closure(re)));
        // @formatter:on
    }

    @Override public IRegExp<S> or(final IRegExp<S> _left, final IRegExp<S> _right) {
        final IRegExp<S> left = _left.match(this);
        final IRegExp<S> right = _right.match(this);
        if(left.equals(right)) {
            return left;
        }
        // @formatter:off
        Supplier<IRegExp<S>> otherwise = () -> right.match(new RegExpCases<S,IRegExp<S>>()
                .or((innerLeft, innerRight) ->
                        (compare(left, innerLeft) > 0)
                                ? or(innerLeft, or(left, innerRight))
                                : builder.or(left, builder.or(innerLeft, innerRight)))
                .otherwise(() -> {
                    return (compare(left, right) > 0) ? or(right, left) : builder.or(left, right);
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

    @Override public IRegExp<S> and(final IRegExp<S> _left, final IRegExp<S> _right) {
        final IRegExp<S> left = _left.match(this);
        final IRegExp<S> right = _right.match(this);
        if(left.equals(right)) {
            return left;
        }
        // @formatter:off
        Supplier<IRegExp<S>> otherwise = () -> right.match(new RegExpCases<S,IRegExp<S>>()
                .and((innerLeft, innerRight) ->
                        (compare(left, innerLeft) > 0)
                                ? and(innerLeft, and(left, innerRight))
                                : builder.and(left, builder.and(innerLeft, innerRight)))
                .otherwise(() -> {
                    return (compare(left, right) > 0) ? and(right, left) : builder.and(left, right);
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

    @Override public IRegExp<S> complement(final IRegExp<S> _re) {
        final IRegExp<S> re = _re.match(this);
        // @formatter:off
        return re.match(new RegExpCases<S,IRegExp<S>>()
                .complement(innerRe -> innerRe)
                .otherwise(() -> builder.complement(re)));
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

}