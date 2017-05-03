package org.metaborg.meta.nabl2.util.collections;

import io.usethesource.capsule.Set;

public final class PSets {

    public static <T> Set.Immutable<T> intersection(Set.Immutable<T> left, Set.Immutable<T> right) {
    	return left.__retainAll(right);
    }

}