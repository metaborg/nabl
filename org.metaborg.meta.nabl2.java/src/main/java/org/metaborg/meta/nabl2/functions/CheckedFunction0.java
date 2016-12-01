package org.metaborg.meta.nabl2.functions;

@FunctionalInterface
public interface CheckedFunction0<R, E extends Throwable> {

    R apply() throws E;

}