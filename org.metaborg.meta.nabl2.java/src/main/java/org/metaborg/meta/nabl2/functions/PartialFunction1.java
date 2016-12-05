package org.metaborg.meta.nabl2.functions;

import java.util.Optional;

@FunctionalInterface
public interface PartialFunction1<T, R> {

    Optional<R> apply(T t);

    @SafeVarargs static <T, R> PartialFunction1<T,R> cases(PartialFunction1<T,R>... fs) {
        return new PartialFunction1<T,R>() {

            @Override public Optional<R> apply(T t) {
                for (PartialFunction1<T,R> f : fs) {
                    Optional<R> r = f.apply(t);
                    if (r.isPresent()) {
                        return r;
                    }
                }
                return Optional.empty();
            }

        };
    }

}