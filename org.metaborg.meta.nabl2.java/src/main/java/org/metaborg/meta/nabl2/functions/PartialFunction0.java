package org.metaborg.meta.nabl2.functions;

import java.util.Optional;

@FunctionalInterface
public interface PartialFunction0<R> {

    Optional<R> apply();

}