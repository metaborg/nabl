package org.metaborg.meta.nabl2.util.functions;

import java.util.Optional;

@FunctionalInterface
public interface PartialFunction3<T1, T2, T3, R> extends Function3<T1, T2, T3, Optional<R>> {

}