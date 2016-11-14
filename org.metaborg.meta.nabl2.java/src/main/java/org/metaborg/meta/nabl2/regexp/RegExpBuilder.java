package org.metaborg.meta.nabl2.regexp;

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
        return left.accept(new ARegExpFunction<S,IRegExp<S>>() {

            @Override public IRegExp<S> emptySet() {
                return RegExpBuilder.this.emptySet();
            }

            @Override public IRegExp<S> emptyString() {
                return right;
            }

            @Override public IRegExp<S> concat(IRegExp<S> innerLeft, IRegExp<S> innerRight) {
                return RegExpBuilder.this.concat(innerLeft, RegExpBuilder.this.concat(innerRight, right));
            }

            @Override public IRegExp<S> defaultValue() {
                return right.accept(new ARegExpFunction<S,IRegExp<S>>() {

                    @Override public IRegExp<S> emptySet() {
                        return RegExpBuilder.this.emptySet();
                    }

                    @Override public IRegExp<S> emptyString() {
                        return left;
                    }

                    @Override public IRegExp<S> defaultValue() {
                        return ImmutableConcat.of(left, right, RegExpBuilder.this);
                    }

                });
            }

        });
    }

    @Override public IRegExp<S> closure(final IRegExp<S> re) {
        return re.accept(new ARegExpFunction<S,IRegExp<S>>() {

            @Override public IRegExp<S> emptySet() {
                return RegExpBuilder.this.emptyString();
            }

            @Override public IRegExp<S> emptyString() {
                return RegExpBuilder.this.emptyString();
            }

            @Override public IRegExp<S> closure(IRegExp<S> innerRe) {
                return ImmutableClosure.of(innerRe, RegExpBuilder.this);
            }

            @Override public IRegExp<S> defaultValue() {
                return ImmutableClosure.of(re, RegExpBuilder.this);
            }

        });
    }

    @Override public IRegExp<S> or(final IRegExp<S> left, final IRegExp<S> right) {
        if (left.equals(right)) {
            return left;
        }
        if (compare(left, right) > 0) {
            return RegExpBuilder.this.or(right, left);
        }
        return left.accept(new ARegExpFunction<S,IRegExp<S>>() {

            @Override public IRegExp<S> or(IRegExp<S> innerLeft, IRegExp<S> innerRight) {
                return ImmutableOr.of(innerLeft, RegExpBuilder.this.or(innerRight, right), RegExpBuilder.this);
            }

            @Override public IRegExp<S> defaultValue() {
                return left.accept(new ARegExpFunction<S,IRegExp<S>>() {

                    @Override public IRegExp<S> emptySet() {
                        return right;
                    }

                    @Override public IRegExp<S> complement(IRegExp<S> re) {
                        final ARegExpFunction<S,IRegExp<S>> outer = this;
                        return re.accept(new ARegExpFunction<S,IRegExp<S>>() {

                            @Override public IRegExp<S> emptySet() {
                                return left;
                            }

                            @Override public IRegExp<S> defaultValue() {
                                return outer.defaultValue();
                            }

                        });
                    }

                    @Override public IRegExp<S> defaultValue() {
                        return ImmutableOr.of(left, right, RegExpBuilder.this);
                    }
                });

            }

        });
    }

    @Override public IRegExp<S> and(final IRegExp<S> left, final IRegExp<S> right) {
        if (left.equals(right)) {
            return left;
        }
        if (compare(left, right) > 0) {
            return RegExpBuilder.this.and(right, left);
        }
        return left.accept(new ARegExpFunction<S,IRegExp<S>>() {

            @Override public IRegExp<S> and(IRegExp<S> innerLeft, IRegExp<S> innerRight) {
                return ImmutableAnd.of(innerLeft, RegExpBuilder.this.and(innerRight, right), RegExpBuilder.this);
            }

            @Override public IRegExp<S> defaultValue() {
                return left.accept(new ARegExpFunction<S,IRegExp<S>>() {

                    @Override public IRegExp<S> emptySet() {
                        return RegExpBuilder.this.emptySet();
                    }

                    @Override public IRegExp<S> complement(IRegExp<S> re) {
                        final ARegExpFunction<S,IRegExp<S>> outer = this;
                        return re.accept(new ARegExpFunction<S,IRegExp<S>>() {

                            @Override public IRegExp<S> emptySet() {
                                return right;
                            }

                            @Override public IRegExp<S> defaultValue() {
                                return outer.defaultValue();
                            }

                        });
                    }

                    @Override public IRegExp<S> defaultValue() {
                        return ImmutableAnd.of(left, right, RegExpBuilder.this);
                    }

                });

            }

        });
    }

    @Override public IRegExp<S> complement(final IRegExp<S> re) {
        return re.accept(new ARegExpFunction<S,IRegExp<S>>() {

            @Override public IRegExp<S> complement(IRegExp<S> innerRe) {
                return innerRe;
            }

            @Override public IRegExp<S> defaultValue() {
                return ImmutableComplement.of(re, RegExpBuilder.this);
            }

        });
    }

    private int compare(final IRegExp<S> re1, final IRegExp<S> re2) {
        return re1.accept(new IRegExpFunction<S,Integer>() {

            @Override public Integer emptySet() {
                return re1.accept(order) - re2.accept(order);
            }

            @Override public Integer emptyString() {
                return re1.accept(order) - re2.accept(order);
            }

            @Override public Integer symbol(final S s1) {
                return re2.accept(new ARegExpFunction<S,Integer>() {

                    @Override public Integer symbol(final S s2) {
                        return alphabet.indexOf(s1) - alphabet.indexOf(s2);
                    }

                    @Override public Integer defaultValue() {
                        return re1.accept(order) - re2.accept(order);
                    }

                });
            }

            @Override public Integer concat(final IRegExp<S> left1, final IRegExp<S> right1) {
                return re2.accept(new ARegExpFunction<S,Integer>() {

                    @Override public Integer concat(final IRegExp<S> left2, final IRegExp<S> right2) {
                        int c = compare(left1, left2);
                        if (c == 0) {
                            c = compare(right1, right2);
                        }
                        return c;
                    }

                    @Override public Integer defaultValue() {
                        return re1.accept(order) - re2.accept(order);
                    }

                });
            }

            @Override public Integer closure(final IRegExp<S> innerRe1) {
                return re2.accept(new ARegExpFunction<S,Integer>() {

                    @Override public Integer closure(final IRegExp<S> innerRe2) {
                        return compare(innerRe1, innerRe2);
                    }

                    @Override public Integer defaultValue() {
                        return re1.accept(order) - re2.accept(order);
                    }

                });
            }

            @Override public Integer or(final IRegExp<S> left1, final IRegExp<S> right1) {
                return re2.accept(new ARegExpFunction<S,Integer>() {

                    @Override public Integer or(final IRegExp<S> left2, final IRegExp<S> right2) {
                        int c = compare(left1, left2);
                        if (c == 0) {
                            c = compare(right1, right2);
                        }
                        return c;
                    }

                    @Override public Integer defaultValue() {
                        return re1.accept(order) - re2.accept(order);
                    }

                });
            }

            @Override public Integer and(final IRegExp<S> left1, final IRegExp<S> right1) {
                return re2.accept(new ARegExpFunction<S,Integer>() {

                    @Override public Integer and(final IRegExp<S> left2, final IRegExp<S> right2) {
                        int c = compare(left1, left2);
                        if (c == 0) {
                            c = compare(right1, right2);
                        }
                        return c;
                    }

                    @Override public Integer defaultValue() {
                        return re1.accept(order) - re2.accept(order);
                    }

                });
            }

            @Override public Integer complement(final IRegExp<S> innerRe1) {
                return re2.accept(new ARegExpFunction<S,Integer>() {

                    @Override public Integer complement(final IRegExp<S> innerRe2) {
                        return compare(innerRe1, innerRe2);
                    }

                    @Override public Integer defaultValue() {
                        return re1.accept(order) - re2.accept(order);
                    }

                });
            }

        });
    }

    private final IRegExpFunction<S,Integer> order = new IRegExpFunction<S,Integer>() {

        @Override public Integer emptySet() {
            return 1;
        }

        @Override public Integer emptyString() {
            return 2;
        }

        @Override public Integer complement(IRegExp<S> re) {
            return 3;
        }

        @Override public Integer closure(IRegExp<S> re) {
            return 4;
        }

        @Override public Integer concat(IRegExp<S> left, IRegExp<S> right) {
            return 5;
        }

        @Override public Integer symbol(S s) {
            return 7 + alphabet.indexOf(s);
        }

        @Override public Integer or(IRegExp<S> left, IRegExp<S> right) {
            return Math.min(left.accept(this), right.accept(this));
        }

        @Override public Integer and(IRegExp<S> left, IRegExp<S> right) {
            return Math.min(left.accept(this), right.accept(this));
        }

    };

}
