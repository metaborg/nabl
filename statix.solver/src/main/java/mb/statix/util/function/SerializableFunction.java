package mb.statix.util.function;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Serializable variant of {@link Function}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
    @Override
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of input to the {@code before} function, and to the
     *           composed function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before}
     * function and then applies this function
     * @throws NullPointerException if before is null
     *
     * @see #andThen(SerializableFunction)
     */
    default <V> SerializableFunction<V, R> compose(SerializableFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }
    
    @Override
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     *
     * @see #compose(SerializableFunction)
     */
    default <V> SerializableFunction<T, V> andThen(SerializableFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> SerializableFunction<T, T> identity() {
        return t -> t;
    }
}
