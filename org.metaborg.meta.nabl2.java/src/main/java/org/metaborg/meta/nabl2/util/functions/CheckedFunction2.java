package org.metaborg.meta.nabl2.util.functions;

@FunctionalInterface
public interface CheckedFunction2<T1, T2, R, E extends Throwable> {

    R apply(T1 t1, T2 t2) throws E;

}