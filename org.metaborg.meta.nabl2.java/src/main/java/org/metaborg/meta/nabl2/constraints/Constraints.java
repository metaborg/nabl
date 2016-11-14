package org.metaborg.meta.nabl2.constraints;

import java.util.Arrays;

public final class Constraints {

    public static IConstraint true_() {
        return ImmutableTrue.of();
    }

    public static IConstraint false_() {
        return ImmutableFalse.of();
    }

    public static IConstraint conj(IConstraint... constraints) {
        return ImmutableConj.of(Arrays.asList(constraints));
    }

    public static IConstraint conj(Iterable<? extends IConstraint> constraints) {
        return ImmutableConj.of(constraints);
    }

}