package org.metaborg.meta.nabl2.util.functions;

@FunctionalInterface
public interface Action1<T> {

    void apply(T t);

}