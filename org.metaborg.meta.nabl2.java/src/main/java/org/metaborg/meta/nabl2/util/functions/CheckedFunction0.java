package org.metaborg.meta.nabl2.util.functions;

@FunctionalInterface
public interface CheckedFunction0<R, E extends Throwable> {

    R apply() throws E;

}