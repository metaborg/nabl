package org.metaborg.meta.nabl2.util.functions;

import java.util.Optional;

@FunctionalInterface
public interface PartialFunction1<T, R> extends Function1<T, Optional<R>> {

}