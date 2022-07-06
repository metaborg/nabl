package mb.scopegraph.regexp.impl;

import com.google.common.collect.ImmutableSet;

import mb.scopegraph.regexp.IAlphabet;
import mb.scopegraph.regexp.IRegExp;

public final class RegExps {

    public static <S> IAlphabet<S> alphabet(IRegExp<S> re) {
        final ImmutableSet.Builder<S> alphabet = ImmutableSet.builder();
        alphabet(re, alphabet);
        return new FiniteAlphabet<>(alphabet.build());
    }

    private static <S> void alphabet(IRegExp<S> re, ImmutableSet.Builder<S> alphabet) {
        new IRegExp.ICases<S, S>() {

            @Override public S emptySet() {
                return null;
            }

            @Override public S emptyString() {
                return null;
            }

            @Override public S symbol(S s) {
                alphabet.add(s);
                return null;
            }

            @Override public S concat(IRegExp<S> left, IRegExp<S> right) {
                alphabet(left, alphabet);
                alphabet(right, alphabet);
                return null;
            }

            @Override public S closure(IRegExp<S> re) {
                alphabet(re, alphabet);
                return null;
            }

            @Override public S or(IRegExp<S> left, IRegExp<S> right) {
                alphabet(left, alphabet);
                alphabet(right, alphabet);
                return null;
            }

            @Override public S and(IRegExp<S> left, IRegExp<S> right) {
                alphabet(left, alphabet);
                alphabet(right, alphabet);
                return null;
            }

            @Override public S complement(IRegExp<S> re) {
                alphabet(re, alphabet);
                return null;
            }

        }.apply(re);
    }

    public static <S> boolean isNullable(IRegExp<S> re) {
        return re.match(new IRegExp.ICases<S, Boolean>() {

            @Override public Boolean emptySet() {
                return false;
            }

            @Override public Boolean emptyString() {
                return true;
            }

            @Override public Boolean symbol(@SuppressWarnings("unused") S s) {
                return false;
            }

            @Override public Boolean concat(IRegExp<S> left, IRegExp<S> right) {
                return left.match(this) && right.match(this);
            }

            @Override public Boolean closure(@SuppressWarnings("unused") IRegExp<S> re) {
                return true;
            }

            @Override public Boolean or(IRegExp<S> left, IRegExp<S> right) {
                return left.match(this) || right.match(this);
            }

            @Override public Boolean and(IRegExp<S> left, IRegExp<S> right) {
                return left.match(this) && right.match(this);
            }

            @Override public Boolean complement(IRegExp<S> re) {
                return !re.match(this);
            }

        });
    }

    public static <S> boolean isOblivion(IRegExp<S> regexp) {
        return regexp.match(new IRegExp.ICases<S, Boolean>() {

            @Override public Boolean emptySet() {
                return true;
            }

            @Override public Boolean emptyString() {
                return false;
            }

            @Override public Boolean symbol(S s) {
                return false;
            }

            @Override public Boolean concat(IRegExp<S> left, IRegExp<S> right) {
                return false;
            }

            @Override public Boolean closure(IRegExp<S> re) {
                return false;
            }

            @Override public Boolean or(IRegExp<S> left, IRegExp<S> right) {
                return false;
            }

            @Override public Boolean and(IRegExp<S> left, IRegExp<S> right) {
                return false;
            }

            @Override public Boolean complement(IRegExp<S> re) {
                return false;
            }
        });
    }

}
