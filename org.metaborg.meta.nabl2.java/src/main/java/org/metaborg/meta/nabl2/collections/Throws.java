package org.metaborg.meta.nabl2.collections;

import java.util.function.Function;

public abstract class Throws<T, E extends Exception> {

    private Throws() {
    }

    public abstract <R> R apply(Matcher<T,E,R> function);

    public <R> R match(Function<? super T,R> success, Function<? super E,R> failure) {
        return apply(Matcher.of(success, failure));
    }

    public <R> Throws<R,E> flatMap(Function<? super T,Throws<R,E>> function) {
        return apply(Matcher.of(function, ex -> failure(ex)));
    }

    public static <T, E extends Exception> Throws<T,E> success(T result) {
        return new Success<T,E>(result);
    }

    public static <T, E extends Exception> Throws<T,E> failure(E exception) {
        return new Failure<T,E>(exception);
    }

    private static final class Success<T, E extends Exception> extends Throws<T,E> {

        private final T result;

        private Success(T result) {
            this.result = result;
        }

        @Override public <R> R apply(Matcher<T,E,R> function) {
            return function.success(result);
        }

    }

    private static final class Failure<T, E extends Exception> extends Throws<T,E> {

        private final E exception;

        private Failure(E exception) {
            this.exception = exception;
        }

        @Override public <R> R apply(Matcher<T,E,R> function) {
            return function.failure(exception);
        }

    }

    public interface Matcher<T, E extends Exception, R> {

        R success(T result);

        R failure(E exception);

        public static <T, E extends Exception, R> Matcher<T,E,R> of(Function<? super T,R> success,
                Function<? super E,R> failure) {
            return new Matcher<T,E,R>() {

                @Override public R success(T result) {
                    return success.apply(result);
                }

                @Override public R failure(E exception) {
                    return failure.apply(exception);
                }

            };
        }

    }

}