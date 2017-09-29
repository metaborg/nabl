package org.metaborg.meta.nabl2.util.functions;

import java.util.Optional;

@FunctionalInterface
public interface PartialFunction2<T1, T2, R> extends Function2<T1, T2, Optional<R>> {

}