package org.metaborg.meta.nabl2.util.functions;

@FunctionalInterface
public interface CheckedFunction4<T1, T2, T3, T4, R, E extends Throwable> {

    R apply(T1 t1, T2 t2, T3 t3, T4 t4) throws E;

}