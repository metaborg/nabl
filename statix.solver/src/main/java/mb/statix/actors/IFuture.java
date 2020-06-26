package mb.statix.actors;

import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface IFuture<T> {

    <U> IFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> handler);

    IFuture<T> whenComplete(BiConsumer<? super T, Throwable> handler);

    T get() throws ExecutionException, InterruptedException;

    boolean isDone();

}