package org.metaborg.meta.nabl2.util.functions;

@FunctionalInterface
public interface Function1<T, R> {

    R apply(T t);

}