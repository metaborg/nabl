package org.metaborg.meta.nabl2.util.functions;

@FunctionalInterface
public interface CheckedPredicate1<T, E extends Throwable> {

    boolean test(T t) throws E;

}